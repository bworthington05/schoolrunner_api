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
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.axis.NumberAxis;
import java.text.NumberFormat;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.renderer.category.StandardBarPainter;
import java.awt.Color;
import java.util.Scanner;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;

//summarizes attendance data for network data digest, generates a .csv and jpeg bar graph
public class AttendanceComparison {
  
  //variables for the min_date & max_date Absences parameters (yyyy-MM-dd)
  private String minDate;
  private Date minDateDate;
  private String prettyMinDate;
  
  private String maxDate;
  private Date maxDateDate;
  private String prettyMaxDate;
  
  private SimpleDateFormat simpleDateFormat;
  private SimpleDateFormat prettyDateFormat;
  
  //variable for the database file path
  private String dbName;
  
  //the actual DatabaseSetup object that creates the database and manages the API connections
  private DatabaseSetup database;
  
  //variable for storing the number of school days within the minDate-maxDate range
  //this is needed for attendance calculations
  private int numberOfSchoolDays;
  
  //file that holds a list of all in session school days
  private String inSessionDaysFile = "/home/ubuntu/workspace/my_github/schoolrunner_api/network_data_digest/import_files/in_session_days.txt";
  
  //constructor that requires the database and String minDate & maxDate (yyyy-MM-dd) for API parameters
  //then takes care of some formatting stuff for minDate and maxDate
  public AttendanceComparison(DatabaseSetup database, String minDate, String maxDate) {
    this.database = database;
    this.minDate = minDate;
    this.maxDate = maxDate;
    this.dbName = this.database.getDatabaseName();

    this.simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    //turn the minDate & maxDate Strings into actual Date objects so they can be reformatted
    try {
      this.minDateDate = simpleDateFormat.parse(this.minDate);
      this.maxDateDate = simpleDateFormat.parse(this.maxDate);
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    
    //set up a pretty date format MM/dd/yyyy that can be included in emails or graph titles
    this.prettyDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    
    //make Strings for minDate & maxDate using the pretty date format
    this.prettyMinDate = prettyDateFormat.format(this.minDateDate);
    this.prettyMaxDate = prettyDateFormat.format(this.maxDateDate);
  } //end constructor
  
  //method that crunches attendance data, makes a stacked barchart JPEG, and returns a String file path of the JPEG
  public String run() {
    
    try {
      //get the number of school days within the minDate-maxDate range
      this.numberOfSchoolDays = getNumberOfSchoolDays();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    
    //file paths for files to be saved
    String csvFileName = "/home/ubuntu/workspace/output/data_digest/Attendance_Comparison_" +
      this.minDate + "-" + this.maxDate + ".csv";
      
    String chartFileNameString = "/home/ubuntu/workspace/output/data_digest/Attendance_Comparison_Graph_" +
      this.minDate + "-" + this.maxDate + ".jpeg";
    
    //custom Absences endpoint url that includes the parameters for active=1, min_date = minDate, and max_date = maxDate
    String absencesEndpoint = "https://renew.schoolrunner.org/api/v1/absences/?limit=30000&active=1&min_date=" +
      this.minDate + "&max_date=" + this.maxDate;
    
    //custom Students endpoint url that includes the parameter active=1
    String studentsEndpoint = "https://renew.schoolrunner.org/api/v1/students/?limit=30000&active=1";
    
    this.database.createAbsencesTable(absencesEndpoint);
    this.database.createAbsenceTypesTable();
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

            //for each school, get the number of specified attendance codes recorded and divde by the number of student days
            //to get the relative percent
            
            "(CAST(SUM(CASE WHEN absence_types.absence_code = 'A' THEN 1 ELSE 0 END) AS FLOAT)/" +
                "(active_students.count_of_students*"+ this.numberOfSchoolDays +")), " +
              
            "(CAST(SUM(CASE WHEN absence_types.absence_code = 'AE' THEN 1 ELSE 0 END) AS FLOAT)/" +
                "(active_students.count_of_students*"+ this.numberOfSchoolDays +")), " +
              
            "(CAST(SUM(CASE WHEN absence_types.absence_code = 'T' THEN 1 ELSE 0 END) AS FLOAT)/" +
                "(active_students.count_of_students*"+ this.numberOfSchoolDays +")), " +
              
            "(CAST(SUM(CASE WHEN absence_types.absence_code = 'TE' THEN 1 ELSE 0 END) AS FLOAST)/" +
                "(active_students.count_of_students*"+ this.numberOfSchoolDays +")), " +
            
            //subtract sum of tardies & absences from (active student count * # of school days in this range, i.e. "student days") 
            //to get the number of "presents", then divide by the number of student days to get the relative percent
            "(CAST(((active_students.count_of_students*"+ this.numberOfSchoolDays +")" +
            "-(SUM(CASE WHEN absence_types.absence_code IN ('A','AE','T','TE') THEN 1 ELSE 0 END))) AS FLOAT)/" +
                "(active_students.count_of_students*"+ this.numberOfSchoolDays +"))" +
            
        "FROM absences " +
            "LEFT OUTER JOIN absence_types ON absences.absence_type_id = absence_types.absence_type_id " +
            "LEFT OUTER JOIN schools ON absences.sr_school_id = schools.sr_school_id " +
            
            //sub-query to get # of active students in each small school
            "LEFT OUTER JOIN ( " +
                "SELECT " +
                    "students.sr_school_id, " +
                    "COUNT(students.sr_school_id) as count_of_students " +
                "FROM students " +
                "WHERE students.active = '1' " +
                "GROUP BY students.sr_school_id " +
            ") active_students ON absences.sr_school_id = active_students.sr_school_id " +

        "WHERE absences.active = '1' AND absences.sr_school_id NOT IN ('5','17','18') " +
        "GROUP BY schools.display_name " +
        "ORDER BY schools.ps_school_id, schools.display_name; ");

      stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      System.out.println("SQLite query complete");
      
      //new filewriter object for saving the data to a .csv
      FileWriter writer = new FileWriter(csvFileName);
      
      //new dataset ob for making a jfreechart graph
      DefaultCategoryDataset dataset = new DefaultCategoryDataset( );
      
      writer.append("School");
	    writer.append(',');
	    writer.append("# of Absent");
	    writer.append(',');
	    writer.append("# of Absent Excused");
	    writer.append(',');
	    writer.append("# of Tardy");
	    writer.append(',');
	    writer.append("# of Tardy Excused");
	    writer.append(',');
	    writer.append("Present");
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
  	    writer.append(',');
  	    writer.append("\"" + rs.getString(6) + "\"");
  	    writer.append('\n');
    	    
        //convert the string values to doubles
        double double2 = Double.parseDouble(rs.getString(2));
        double double3 = Double.parseDouble(rs.getString(3));
        double double4 = Double.parseDouble(rs.getString(4));
        double double5 = Double.parseDouble(rs.getString(5));
        
        //last value is (student days - tardies & absences) = everyone else, AKA "Present"
        //which is the remaining top part of the stacked barchart (everyone else who wasnt absent or tardy)
        double double6 = Double.parseDouble(rs.getString(6));
        
        //add each row of the resultset to the dataset (value, attendance type, school)
        //reorder so that these records are in order we want the bars in the graph: P, TE, T, AE, A
        dataset.addValue(double6, "Present", rs.getString(1));
        dataset.addValue(double5, "Tardy Excused", rs.getString(1));
        dataset.addValue(double4, "Tardy", rs.getString(1));
        dataset.addValue(double3, "Absent Excused", rs.getString(1));
        dataset.addValue(double2, "Absent", rs.getString(1));
      }
      
      //make the stacked barchart
      JFreeChart chart = ChartFactory.createStackedBarChart(
        "Network Attendance Comparison\n" + this.prettyMinDate + " - " + this.prettyMaxDate + 
          " (" + this.numberOfSchoolDays + " School Days)", //graph title
        "Small School", //legend label
        "Avg % of Students per Day", //vertical axis label 
        dataset, //dataset being used
        PlotOrientation.VERTICAL, //bar orientation
        true, //include legend
        true, //generate tooltips
        false); //generate URLs
      
      //create renderer to customize the chart
      CategoryPlot plot = chart.getCategoryPlot();
      StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
      renderer.setBarPainter(new StandardBarPainter());
      
      //make the dashed lines that go across the chart black
      plot.setRangeGridlinePaint(Color.BLACK);
      
      plot.setBackgroundPaint(Color.WHITE);
      
      //set the color of each series using custom colors from MyColors class
      renderer.setSeriesPaint(0, MyColors.GREEN);
      renderer.setSeriesPaint(1, MyColors.LIGHT_GREEN);
      renderer.setSeriesPaint(2, MyColors.YELLOW);
      renderer.setSeriesPaint(3, MyColors.ORANGE);
      renderer.setSeriesPaint(4, MyColors.RED);
      
      //renders each section of a bar as a percent out of 100
      renderer.setRenderAsPercentages(true);
      
      NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
      
      //set the range to be between 0 and 1.0 (or 0% and 100%)
      rangeAxis.setRange(0.0, 1.0);
      
      //set the range axis (Y axis) scale to show percents as whole numbers
      rangeAxis.setNumberFormatOverride(NumberFormat.getPercentInstance());
      
      //set the category labels on the X axis to be written vertically
      CategoryAxis categoryAxis = (CategoryAxis) plot.getDomainAxis();
      categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
      
      //generate the value labels for each section of the bar
      renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", NumberFormat.getPercentInstance()));
      renderer.setBaseItemLabelsVisible(true);
         
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
  
  //method that determines how many school days were within the range minDate-maxDate
  private int getNumberOfSchoolDays() throws FileNotFoundException {
  
    ArrayList<String> schoolDays = new ArrayList<String>();
    
    //read the file that contains the list of in session school days
    Scanner scanner = new Scanner(new File(this.inSessionDaysFile));
    
    while(scanner.hasNext()) {
        
        String date = scanner.next();
        
        //if minDate <= scanner.next() <= maxDate, then add scanner.next()'s date to the array of school days
        if (((this.minDate.compareToIgnoreCase(date)) <= 0) && ((this.maxDate.compareToIgnoreCase(date)) >= 0)) {
          schoolDays.add(date);
        }
        
    } //end while loop
    
    scanner.close();
    
    //return the size of the array, which is the number of school days within the rande minDate-maxDate
    return schoolDays.size();

  } //end getNumberOfSchoolDayes method
  
  //method to get the String message that should go above the chart in an email
  public String getEmailMessage() {
    
    String message = "<strong>This chart shows a relative comparison of absence and tardy rates across the " +
      "network for the date range shown. It is based on daily attendance data recorded in Schoolrunner.</strong><br><br>.";
    
    return message;
  }
  
  public String getAnalysisLinks() {
    
    String rcaaPK2link = "https://renew.schoolrunner.org/analysis/?analysis_report_id=743";
    
    String links = "<strong>For a more detailed breakdown of attendance data by grade level and student, " +
                    "take a look at your school's attendance report in Schoolrunner:</strong><br>" +
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
