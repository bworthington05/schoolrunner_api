import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.axis.NumberAxis;
import java.text.NumberFormat;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.renderer.category.StandardBarPainter;
import java.awt.Color;
import java.util.Scanner;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.title.TextTitle;

//summarizes suspension rates by quarter for network data digest, generates a .csv and jpeg bar graph
public class SuspensionComparison {
  
  //today's date used in graph title and .CSV filename
  private String today;
  private Date todaysDate;
  private String prettyToday;
  
  private String q1StartDate; //q1StartDate is min_date for URL parameter
  private String q2StartDate;
  private String q3StartDate;
  private String q4StartDate;
  
  private SimpleDateFormat simpleDateFormat;
  private SimpleDateFormat prettyDateFormat;
  
  //variable for the database file path
  private String dbName;
  
  //the actual DatabaseSetup object that creates the database and manages the API connections
  private DatabaseSetup database;
  
  //variable for storing the number of school days within each quarter that have been completed so far
  //this is used to calculate average number of suspension days per student days
  private int q1SchoolDays = 0;
  private int q2SchoolDays = 0;
  private int q3SchoolDays = 0;
  private int q4SchoolDays = 0;
  
  //file that holds a list of all in session school days
  private String inSessionDaysFile = "/home/ubuntu/workspace/my_github/schoolrunner_api/network_data_digest/import_files/in_session_days.txt";
  
  //the "order by" for SQL query (also the order of the bars in the chart)
  private String SQLiteOrderBy;
  
  //constructor that requires the database and String dates for start of each quarter (yyyy-MM-dd) for API parameters
  //also requires a String "order by" statement for the SQL query
  public SuspensionComparison(DatabaseSetup database, String q1StartDate, String q2StartDate, String q3StartDate, 
    String q4StartDate, String SQLiteOrderBy) {
    this.database = database;
    this.q1StartDate = q1StartDate;
    this.q2StartDate = q2StartDate;
    this.q3StartDate = q3StartDate;
    this.q4StartDate = q4StartDate;
    this.dbName = this.database.getDatabaseName();

    this.simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    //get today's date so that it can be added to file names and graphs
    this.todaysDate = Calendar.getInstance().getTime();
    this.simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    this.today = simpleDateFormat.format(this.todaysDate);
    
    //set up a pretty date format MM/dd/yyyy that can be included in emails or graph titles
    this.prettyDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    
    //make String for today's date using the pretty date format
    this.prettyToday = prettyDateFormat.format(this.todaysDate);
    
    this.SQLiteOrderBy = SQLiteOrderBy;
  } //end constructor
  
  //method that crunches suspension data, makes a stacked barchart JPEG, and returns a String file path of the JPEG
  public String run() {
    
    try {
      
      //call the method that will count how many school days are in each quarter
      getNumberOfSchoolDays();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    
    //file paths for files to be saved
    String csvFileName = "/home/ubuntu/workspace/output/data_digest/Suspension_Comparison_" + this.today + ".csv";
      
    String chartFileNameString = "/home/ubuntu/workspace/output/data_digest/Suspension_Comparison_Graph_" + this.today + ".jpeg";
    
    //custom Incidents-Suspensions endpoint url that uses q1StartDate as the min_date
    String incidentSuspensionsEndpoint = "https://renew.schoolrunner.org/api/v1/incidents?limit=30000&active=1&with_related=true&min_date=" + this.q1StartDate;
    
    //custom Incidents endpoint url that uses q1StartDate as the min_date
    String incidentsEndpoint = "https://renew.schoolrunner.org/api/v1/incidents?limit=30000&active=1&min_date=" + this.q1StartDate;
    
    //custom Students endpoint url that includes the parameter active=1
    String studentsEndpoint = "https://renew.schoolrunner.org/api/v1/students/?limit=30000&active=1";
    
    this.database.createIncidentSuspensionsTable(incidentSuspensionsEndpoint);
    this.database.createIncidentsTable(incidentsEndpoint);
    this.database.createSchoolsTable();
    this.database.createStudentsTable(studentsEndpoint);
    
    Connection c = null;
    Statement stmt = null;
    
    try {
      
      Class.forName("org.sqlite.JDBC");
      c = DriverManager.getConnection("jdbc:sqlite:" + this.dbName);
      c.setAutoCommit(false);
      System.out.println("opened database successfully");
      
      String query = (
        "SELECT " +
            "replace(schools.display_name, 'Pre-K', 'PK'), " +

            //for each school, get the total number of suspension days per quarter
            //then divide by the number of completed student days (student count X completed school days) in each quarter
            //multiply the whole result by 10,000 to get suspension days per 10,000 student days (should be enough to round to a whole number)
            "ROUND((IFNULL(CAST(q1.suspension_days AS FLOAT) / NULLIF((active_students.count_of_students * " + this.q1SchoolDays +" ), 0), 0) * 10000), 0), " +
            "ROUND((IFNULL(CAST(q2.suspension_days AS FLOAT) / NULLIF((active_students.count_of_students * " + this.q2SchoolDays +" ), 0), 0) * 10000), 0), " +
            "ROUND((IFNULL(CAST(q3.suspension_days AS FLOAT) / NULLIF((active_students.count_of_students * " + this.q3SchoolDays +" ), 0), 0) * 10000), 0), " +
            "ROUND((IFNULL(CAST(q4.suspension_days AS FLOAT) / NULLIF((active_students.count_of_students * " + this.q4SchoolDays +" ), 0), 0) * 10000), 0) " + 
                
        "FROM incident_suspensions " +
            "LEFT OUTER JOIN incidents ON incident_suspensions.incident_id = incidents.incident_id " +
            "LEFT OUTER JOIN schools ON incidents.sr_school_id = schools.sr_school_id " +
            
            //sub-query to get # of active students in each small school
            "LEFT OUTER JOIN ( " +
                "SELECT " +
                    "students.sr_school_id, " +
                    "COUNT(students.sr_school_id) as count_of_students " +
                "FROM students " +
                "WHERE students.active = '1' " +
                "GROUP BY students.sr_school_id " +
            ") active_students ON incidents.sr_school_id = active_students.sr_school_id " +
            
              //sub-query to get total # of suspension days in Q1
              "LEFT JOIN ( " +
                  "SELECT " +
                      "incidents.sr_school_id, " +
                      "SUM(incident_suspensions.num_days) as suspension_days " +
                  "FROM incident_suspensions " +
                      "LEFT JOIN incidents ON incident_suspensions.incident_id = incidents.incident_id " +
                      "LEFT JOIN schools ON incidents.sr_school_id = schools.sr_school_id " +
                      
                  //make sure BOTH the actual incident and suspension are active
                  "WHERE incidents.active = '1' AND incident_suspensions.active = '1'" +
                  
                  //just count incidents between start of one quarter (inclusive) and start of next quarter (exclusive)
                  "AND incidents.date >= '" + this.q1StartDate + "' " +
                  "AND incidents.date < '" + this.q2StartDate + "' " +
                  
                  //only count OSS consequence/suspension types (suspension_type_id 1)
                  "AND incident_suspensions.suspension_type_id = '1' " +
                  "GROUP BY incidents.sr_school_id " +
              ") q1 ON incidents.sr_school_id = q1.sr_school_id " +
              
              //sub-query to get total # of suspension days in Q2
              "LEFT JOIN ( " +
                  "SELECT " +
                      "incidents.sr_school_id, " +
                      "SUM(incident_suspensions.num_days) as suspension_days " +
                  "FROM incident_suspensions " +
                      "LEFT JOIN incidents ON incident_suspensions.incident_id = incidents.incident_id " +
                      "LEFT JOIN schools ON incidents.sr_school_id = schools.sr_school_id " +
                      
                  //make sure BOTH the actual incident and suspension are active
                  "WHERE incidents.active = '1' AND incident_suspensions.active = '1'" +
                  
                  //just count incidents between start of one quarter (inclusive) and start of next quarter (exclusive)
                  "AND incidents.date >= '" + this.q2StartDate + "' " +
                  "AND incidents.date < '" + this.q3StartDate + "' " +
                  
                  //only count OSS consequence/suspension types (suspension_type_id 1)
                  "AND incident_suspensions.suspension_type_id = '1' " +
                  "GROUP BY incidents.sr_school_id " +
              ") q2 ON incidents.sr_school_id = q2.sr_school_id " +
              
              //sub-query to get total # of suspension days in Q3
              "LEFT JOIN ( " +
                  "SELECT " +
                      "incidents.sr_school_id, " +
                      "SUM(incident_suspensions.num_days) as suspension_days " +
                  "FROM incident_suspensions " +
                      "LEFT JOIN incidents ON incident_suspensions.incident_id = incidents.incident_id " +
                      "LEFT JOIN schools ON incidents.sr_school_id = schools.sr_school_id " +
                      
                  //make sure BOTH the actual incident and suspension are active
                  "WHERE incidents.active = '1' AND incident_suspensions.active = '1'" +
                  
                  //just count incidents between start of one quarter (inclusive) and start of next quarter (exclusive)
                  "AND incidents.date >= '" + this.q3StartDate + "' " +
                  "AND incidents.date < '" + this.q4StartDate + "' " +
                  
                  //only count OSS consequence/suspension types (suspension_type_id 1)
                  "AND incident_suspensions.suspension_type_id = '1' " +
                  "GROUP BY incidents.sr_school_id " +
              ") q3 ON incidents.sr_school_id = q3.sr_school_id " +
              
              //sub-query to get total # of suspension days in Q4
              "LEFT JOIN ( " +
                  "SELECT " +
                      "incidents.sr_school_id, " +
                      "SUM(incident_suspensions.num_days) as suspension_days " +
                  "FROM incident_suspensions " +
                      "LEFT JOIN incidents ON incident_suspensions.incident_id = incidents.incident_id " +
                      "LEFT JOIN schools ON incidents.sr_school_id = schools.sr_school_id " +
                      
                  //make sure BOTH the actual incident and suspension are active
                  "WHERE incidents.active = '1' AND incident_suspensions.active = '1'" +
                  
                  //just count incidents greater than or equal to Q4 start date
                  "AND incidents.date >= '" + this.q4StartDate + "' " +
                  
                  //only count OSS consequence/suspension types (suspension_type_id 1)
                  "AND incident_suspensions.suspension_type_id = '1' " +
                  "GROUP BY incidents.sr_school_id " +
              ") q4 ON incidents.sr_school_id = q4.sr_school_id " +
        
        //don't include RAHS, ECC, or RSP
        "WHERE incidents.active = '1' AND incidents.sr_school_id NOT IN ('5','17','18') " +
        "GROUP BY schools.display_name " +
        "ORDER BY " + this.SQLiteOrderBy + "; ");

      stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      System.out.println("SQLite query complete");
      
      //new filewriter object for saving the data to a .csv
      FileWriter writer = new FileWriter(csvFileName);
      
      //new dataset ob for making a jfreechart graph
      DefaultCategoryDataset dataset = new DefaultCategoryDataset( );
      
      writer.append("School");
	    writer.append(',');
	    writer.append("Q1 Suspension Rate");
	    writer.append(',');
	    writer.append("Q2 Suspension Rate");
	    writer.append(',');
	    writer.append("Q3 Suspension Rate");
	    writer.append(',');
	    writer.append("Q4 Suspension Rate");
	    writer.append('\n');
      
      //loop throw the SQL resultset and save records to the filewriter and jfreechart dataset
      while (rs.next()) {
          
        //add each record of the SQL results to the FileWriter object
        //surrounding quotes "\"" used to ensure data that my contain commas don't act as delimiters
        writer.append("\"" + rs.getString(1) + "\"");
  	    writer.append(',');
  	    writer.append("\"" + rs.getString(2) + "\"");
  	    writer.append(',');
  	    writer.append("\"" + rs.getString(3) + "\"");
  	    writer.append(',');
  	    writer.append("\"" + rs.getString(4) + "\"");
  	    writer.append(',');
  	    writer.append("\"" + rs.getString(5) + "\"");
  	    writer.append('\n');
    	    
        //convert the string values to doubles
        Double double2 = Double.parseDouble(rs.getString(2));
        Double double3 = Double.parseDouble(rs.getString(3));
        Double double4 = Double.parseDouble(rs.getString(4));
        Double double5 = Double.parseDouble(rs.getString(5));
        
        //if any values are 0 (or super close to 0), set to null so 0 is not displayed on graph
        if (double2 < 0.001)
          double2 = null;
          
        if (double3 < 0.001)
          double3 = null;
          
        if (double4 < 0.001)
          double4 = null;
          
        if (double5 < 0.001)
          double5 = null;
        
        //add each row of the resultset to the dataset (value, quarter, school)
        dataset.addValue(double2, "Quarter 1", rs.getString(1));
        dataset.addValue(double3, "Quarter 2", rs.getString(1));
        dataset.addValue(double4, "Quarter 3", rs.getString(1));
        dataset.addValue(double5, "Quarter 4", rs.getString(1));
      }
      
      //make the stacked barchart
      JFreeChart chart = ChartFactory.createBarChart(
        null, //graph title is null (will be set later)
        "Small School", //legend label
        "OSS Rate (Suspension Days per 10,000 Student Days)", //vertical axis label 
        dataset, //dataset being used
        PlotOrientation.VERTICAL, //bar orientation
        true, //include legend
        true, //generate tooltips
        false); //generate URLs
      
      //title of the chart
      String titleString = "Network Suspension Rate Comparison\n as of " + this.prettyToday;
      
      //format the title
      TextTitle title = new TextTitle(titleString, MyFonts.TITLE_FONT);
      chart.setTitle(title);
      
      //create renderer to customize the chart
      CategoryPlot plot = chart.getCategoryPlot();
      BarRenderer renderer = (BarRenderer) plot.getRenderer();
      renderer.setBarPainter(new StandardBarPainter());
      
      //make the dashed lines that go across the chart black
      plot.setRangeGridlinePaint(Color.BLACK);
      
      plot.setBackgroundPaint(Color.WHITE);
      
      //set the color of each series using custom colors from MyColors class
      renderer.setSeriesPaint(0, MyColors.LIGHT_PURPLE);
      renderer.setSeriesPaint(1, MyColors.YELLOW);
      renderer.setSeriesPaint(2, MyColors.LIGHT_GREEN);
      renderer.setSeriesPaint(3, MyColors.BLUE);
      
      //set the category labels on the X axis to be written vertically
      CategoryAxis categoryAxis = (CategoryAxis) plot.getDomainAxis();
      categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
      
      //generate the value labels for each section of the bar
      renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());
      renderer.setBaseItemLabelsVisible(true);
      renderer.setBaseItemLabelFont(MyFonts.ITEM_LABEL_FONT);
         
      int width = 768; //width of the image
      int height = 475; //height of the image
      File chartFileName = new File(chartFileNameString);
      ChartUtilities.saveChartAsJPEG(chartFileName, chart, width, height);
      
      rs.close();
      stmt.close();
      c.close();

	    writer.flush();
	    writer.close();
	    
	    //print confirmation that files were saved
	    System.out.println("files saved successfully\n");
      
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
    }
    
    return chartFileNameString;
    
  } //end run method
  
  //method that determines how many school days have been completed in each quarter
  //increments the instance variables q1SchoolDays, q2SchoolDays, etc. used elsewhere in this class
  private void getNumberOfSchoolDays() throws FileNotFoundException {
    
    //read the file that contains the list of in session school days
    Scanner scanner = new Scanner(new File(this.inSessionDaysFile));
    
    while(scanner.hasNext()) {
        
        String date = scanner.next();
        
        //if date is within the range of a given quarter, increment the schooldays counter for that quarter
        //first make sure the date is not in the future (only count completed school days)
        if (date.compareToIgnoreCase(this.today) <= 0) {
          
          //if date is less than q2StartDate, then it's in q1
          if (date.compareToIgnoreCase(this.q2StartDate) < 0) {
            this.q1SchoolDays++;
          }
          
          //else if date is less than q3StartDate, then it's in q2
          else if (date.compareToIgnoreCase(this.q3StartDate) < 0) {
            this.q2SchoolDays++;
          }
          
          else if (date.compareToIgnoreCase(this.q4StartDate) < 0) {
            this.q3SchoolDays++;
          }
          
          else {
            this.q4SchoolDays++;
          }
          
        } //end big if
        
    } //end while loop
    
    scanner.close();
    
    System.out.println("Q1 completed schooldays = " + this.q1SchoolDays);
    System.out.println("Q2 completed schooldays = " + this.q2SchoolDays);
    System.out.println("Q3 completed schooldays = " + this.q3SchoolDays);
    System.out.println("Q4 completed schooldays = " + this.q4SchoolDays);

  } //end getNumberOfSchoolDayes method
  
  //method to get the String message that should go above the chart in an email
  public String getEmailMessage() {
    
    String message = "<i><font size='3'>This chart shows the relative rate of out-of-school suspensions at each school by quarter. " +
      "Specifically, it shows the number of OSS days per 10,000 student days in school, which equates to approximately " +
      "a month of instructional days for a school with 500 students. It is based on incidents/suspensions " +
      "recorded in Schoolrunner.</i></font size='3'><br><br>.";
    
    return message;
  }
  
  public String getAnalysisLinks() {
    
    String rcaaPK2link = "https://renew.schoolrunner.org/analysis/?analysis_report_id=743";
    
    String links = "<strong>For a more detailed breakdown of incident/suspension data by grade level and student, " +
                    "take a look at your school's incidents report in Schoolrunner:</strong><br>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>RCAA PK-2</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>RCAA 3-4</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>RCAA 5-8</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>STA PK-2</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>STA 3-8</font size='3'></strong></a><br>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>DTA PK-2</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>DTA 3-5</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>DTA 6-8</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>SCH PK-3</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>SCH 4-5</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>SCH 6-8</font size='3'></strong></a><br>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>MCPA PK-4</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaaPK2link + "'><strong><font size='3'>MCPA 5-8</font size='3'></strong></a><br><br>";
    return links;
  }
  
} //end class
