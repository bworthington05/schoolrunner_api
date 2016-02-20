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
import java.util.Scanner;
import java.awt.Color;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.title.TextTitle;

//calculates the average number of assessment results per student
//for each core content course at each school, generates a .csv and jpeg bar graph
public class AssessmentComparisonRelative {
  
  //variables for the min_date & max_date Absences parameters (yyyy-MM-dd)
  private String minDate;
  private Date minDateDate;
  private String prettyMinDate;
  
  private String maxDate;
  private Date maxDateDate;
  private String prettyMaxDate;
  
  //used in SQL query to get list of term-bin IDs for the custom Assessment-Students endpoint
  private String termBinStartDate;
  
  //use to store a list of term-bin IDs that will be appened to Assessment-Students endpoint URL
  private String termBinsIDList;
  
  private SimpleDateFormat simpleDateFormat;
  private SimpleDateFormat prettyDateFormat;
  
  //variable for the database file path
  private String dbName;
  
  //the actual DatabaseSetup object that creates the database and manages the API connections
  private DatabaseSetup database;
  
  //variables for storing String list of core content course names
  private String nfCourseList;
  private String elaCourseList;
  private String mathCourseList;
  private String sciCourseList;
  private String ssCourseList;
  
  //file that holds a list of all the core content course names (required for SQL query)
  private String courseListFile = "/home/ubuntu/workspace/my_github/schoolrunner_api/network_data_digest/import_files/core_content_courses.txt";
  
  //the "order by" for SQL query (also the order of the bars in the chart)
  private String SQLiteOrderBy;  
  
  //constructor that requires the database and String minDate & maxDate & termBinStartDate (yyyy-MM-dd) for API parameters
  //also requires a String "order by" statement for the SQL query
  //then takes care of some formatting stuff for minDate and maxDate
  public AssessmentComparisonRelative(DatabaseSetup database, String minDate, String maxDate, String termBinStartDate, String SQLiteOrderBy) {
    this.database = database;
    this.minDate = minDate;
    this.maxDate = maxDate;
    this.termBinStartDate = termBinStartDate;
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
    
    this.SQLiteOrderBy = SQLiteOrderBy;
  } //end constructor
  
  //method that crunches assessment data, makes a stacked barchart JPEG, and returns a String file path of the JPEG
  public String run() {
    
    try {
      //get list of core content courses for each subject
      getCourseList();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    
    //file paths for files to be saved
    String csvFileName = "/home/ubuntu/workspace/output/data_digest/Assessment_Comparison_Relative_" +
      this.minDate + "-" + this.maxDate + ".csv";
      
    String chartFileNameString = "/home/ubuntu/workspace/output/data_digest/Assessment_Comparison_Graph_Relative_" +
      this.minDate + "-" + this.maxDate + ".jpeg";
    
    //custom Assessment endpoint url that includes the parameters for active=1, min_date = minDate, and max_date = maxDate
    String assessmentEndpoint = "https://renew.schoolrunner.org/api/v1/assessments/?limit=30000&active=1&min_date=" +
      this.minDate + "&max_date=" + this.maxDate;
      
    //custom Students endpoint url that includes the parameter active=1
    String studentsEndpoint = "https://renew.schoolrunner.org/api/v1/students/?limit=30000&active=1";    
    
    this.database.createAssessmentsTable(assessmentEndpoint);
    this.database.createCoursesTable();
    this.database.createSchoolsTable();
    this.database.createStudentsTable(studentsEndpoint);
    this.database.createTermBinsTable();
    
    //get String list of term_bin_ids
    getTermBinIDs();
    
    //custom Assessment-Students endpoint url that includes the parameters for active=1 and term_bin_ids
    String assessmentStudentsEndpoint = "https://renew.schoolrunner.org/api/v1/assessment-students/?limit=30000&active=1&term_bin_ids=" +
      this.termBinsIDList;
    
    //connect to Assessment-Students API AFTER the custom endpoint is updated  
    this.database.createAssessmentStudentsTable(assessmentStudentsEndpoint);
    
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
            
            //get the number of student assessment results for each subject
            //and divide by the active student count to get the number of results per student in each subject
            //round the result to 0 decimal places
            
            "ROUND((CAST(IFNULL(nf.assessment_count, '0') AS FLOAT)/active_students.count_of_students), 0), " +
            
            "ROUND((CAST(IFNULL(ela.assessment_count, '0') AS FLOAT)/active_students.count_of_students), 0)," +
            
            "ROUND((CAST(IFNULL(math.assessment_count, '0') AS FLOAT)/active_students.count_of_students), 0), " +
            
            "ROUND((CAST(IFNULL(sci.assessment_count, '0') AS FLOAT)/active_students.count_of_students), 0), " +
            
            "ROUND((CAST(IFNULL(ss.assessment_count, '0') AS FLOAT)/active_students.count_of_students), 0) " +
            
        "FROM assessment_students " +
            "LEFT OUTER JOIN assessments ON assessment_students.assessment_id = assessments.assessment_id " +
            "LEFT OUTER JOIN schools ON assessments.sr_school_id = schools.sr_school_id " +
            
            //sub-query to get # of Nonfiction assessment student results for each school
            "LEFT OUTER JOIN ( " +
                "SELECT " +
                    "assessments.sr_school_id, " +
                    "COUNT(assessments.sr_school_id) as assessment_count " +
                "FROM assessment_students " +
                  "LEFT OUTER JOIN assessments ON assessment_students.assessment_id = assessments.assessment_id " +
                  "LEFT OUTER JOIN courses ON assessments.course_id = courses.course_id " +
                "WHERE assessments.active = '1' AND courses.course_name IN " + this.nfCourseList + " " +
                //don't count District Benchmarks (12) or MLQs (8)
                "AND assessments.assessment_type_id NOT IN ('12','8') " +
                "AND assessment_students.missing = '0'" +
                "AND assessment_students.active = '1'" +
                "GROUP BY assessments.sr_school_id " +
            ") nf ON assessments.sr_school_id = nf.sr_school_id " +
            
            //sub-query to get # of ELA assessment student results for each schooll
            "LEFT OUTER JOIN ( " +
                "SELECT " +
                    "assessments.sr_school_id, " +
                    "COUNT(assessments.sr_school_id) as assessment_count " +
                "FROM assessment_students " +
                  "LEFT OUTER JOIN assessments ON assessment_students.assessment_id = assessments.assessment_id " +
                  "LEFT OUTER JOIN courses ON assessments.course_id = courses.course_id " +
                "WHERE assessments.active = '1' AND courses.course_name IN " + this.elaCourseList + " " +
                //don't count District Benchmarks (12) or MLQs (8)
                "AND assessments.assessment_type_id NOT IN ('12','8') " +
                "AND assessment_students.missing = '0'" +
                "AND assessment_students.active = '1'" +
                "GROUP BY assessments.sr_school_id " +
            ") ela ON assessments.sr_school_id = ela.sr_school_id " +
            
            //sub-query to get # of math assessment student results for each school
            "LEFT OUTER JOIN ( " +
                "SELECT " +
                    "assessments.sr_school_id, " +
                    "COUNT(assessments.sr_school_id) as assessment_count " +
                "FROM assessment_students " +
                  "LEFT OUTER JOIN assessments ON assessment_students.assessment_id = assessments.assessment_id " +
                  "LEFT OUTER JOIN courses ON assessments.course_id = courses.course_id " +
                "WHERE assessments.active = '1' AND courses.course_name IN " + this.mathCourseList + " " +
                //don't count District Benchmarks (12) or MLQs (8)
                "AND assessments.assessment_type_id NOT IN ('12','8') " +
                "AND assessment_students.missing = '0'" +
                "AND assessment_students.active = '1'" +
                "GROUP BY assessments.sr_school_id " +
            ") math ON assessments.sr_school_id = math.sr_school_id " +
            
            //sub-query to get # of science assessment student results for each school
            "LEFT OUTER JOIN ( " +
                "SELECT " +
                    "assessments.sr_school_id, " +
                    "COUNT(assessments.sr_school_id) as assessment_count " +
                "FROM assessment_students " +
                  "LEFT OUTER JOIN assessments ON assessment_students.assessment_id = assessments.assessment_id " +
                  "LEFT OUTER JOIN courses ON assessments.course_id = courses.course_id " +
                "WHERE assessments.active = '1' AND courses.course_name IN " + this.sciCourseList + " " +
                //don't count District Benchmarks (12) or MLQs (8)
                "AND assessments.assessment_type_id NOT IN ('12','8') " +
                "AND assessment_students.missing = '0'" +
                "AND assessment_students.active = '1'" +
                "GROUP BY assessments.sr_school_id " +
            ") sci ON assessments.sr_school_id = sci.sr_school_id " +
            
            //sub-query to get # of social studies assessment student results for each school
            "LEFT OUTER JOIN ( " +
                "SELECT " +
                    "assessments.sr_school_id, " +
                    "COUNT(assessments.sr_school_id) as assessment_count " +
                "FROM assessment_students " +
                  "LEFT OUTER JOIN assessments ON assessment_students.assessment_id = assessments.assessment_id " +
                  "LEFT OUTER JOIN courses ON assessments.course_id = courses.course_id " +
                "WHERE assessments.active = '1' AND courses.course_name IN " + this.ssCourseList + " " +
                //don't count District Benchmarks (12) or MLQs (8)
                "AND assessments.assessment_type_id NOT IN ('12','8') " +
                "AND assessment_students.missing = '0'" +
                "AND assessment_students.active = '1'" +
                "GROUP BY assessments.sr_school_id " +
            ") ss ON assessments.sr_school_id = ss.sr_school_id " +
        
            //sub-query to get # of active students in each small school
            "LEFT OUTER JOIN ( " +
                "SELECT " +
                    "students.sr_school_id, " +
                    "COUNT(students.sr_school_id) as count_of_students " +
                "FROM students " +
                "WHERE students.active = '1' " +
                "GROUP BY students.sr_school_id " +
            ") active_students ON assessments.sr_school_id = active_students.sr_school_id " +        
        
        //don't count RAHS, ECC, or RSP
        "WHERE assessments.active = '1' AND assessments.sr_school_id NOT IN ('5','17','18') " +
        //don't count District Benchmarks (12) or MLQs (8)
        "AND assessments.assessment_type_id NOT IN ('12','8') " +
        "AND assessment_students.missing = '0'" +
        "AND assessment_students.active = '1'" +
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
	    writer.append("# of NF Assessments");
	    writer.append(',');
	    writer.append("# of ELA Assessments");
	    writer.append(',');
	    writer.append("# of Math Assessments");
	    writer.append(',');
	    writer.append("# of Science Assessments");
	    writer.append(',');
	    writer.append("# of SS Assessments");
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
        double double6 = Double.parseDouble(rs.getString(6));
        
        //add each row of the resultset to the dataset (value, school name, attendance type)
        dataset.addValue(double2, "Nonfiction", rs.getString(1));
        dataset.addValue(double3, "ELA", rs.getString(1));
        dataset.addValue(double4, "Math", rs.getString(1));
        dataset.addValue(double5, "Science", rs.getString(1));
        dataset.addValue(double6, "Social Studies", rs.getString(1));
      }
      
      //make the stacked barchart
      JFreeChart chart = ChartFactory.createStackedBarChart(
        null, //graph title is null (will be set later)
        "Small School", //legend label
        "Avg # of Assessment Results per Student", //vertical axis label 
        dataset, //dataset being used
        PlotOrientation.VERTICAL, //bar orientation
        true, //include legend
        true, //generate tooltips
        false); //generate URLs
      
      //title of the chart
      String titleString = "Teacher-Created Assessment Rate Comparison\n" + this.prettyMinDate + " - " + this.prettyMaxDate;
      
      //format the title
      TextTitle title = new TextTitle(titleString, MyFonts.TITLE_FONT);
      chart.setTitle(title);
      
      //create renderer to customize the chart
      CategoryPlot plot = chart.getCategoryPlot();
      StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
      renderer.setBarPainter(new StandardBarPainter());
      
      //make the dashed lines that go across the chart black
      plot.setRangeGridlinePaint(Color.BLACK);
      
      plot.setBackgroundPaint(Color.WHITE);
      
      //set the color of each series using custom colors from MyColors class
      renderer.setSeriesPaint(0, MyColors.LIGHT_PURPLE);
      renderer.setSeriesPaint(1, MyColors.LIGHT_GREEN);
      renderer.setSeriesPaint(2, MyColors.BLUE);
      renderer.setSeriesPaint(3, MyColors.ORANGE);
      renderer.setSeriesPaint(4, MyColors.LIGHT_BLUE);
      
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
  
  //method that makes separate lists of core content course names for each subject
  //lists are fomatted at "(a, b, c)" so that they can be used in SQL query
  private void getCourseList() throws FileNotFoundException {
  
    this.nfCourseList = "(";
    this.elaCourseList = "(";
    this.mathCourseList = "(";
    this.sciCourseList = "(";
    this.ssCourseList = "(";
    
    //read the file that contains the list of core content course names
    Scanner scanner = new Scanner(new File(this.courseListFile));
    
    //use hasNextLine() and nextLine() so that the whole course name is returned since there are spaces in the name
    while(scanner.hasNextLine()) {
        
        String course = scanner.nextLine();
        
        //if the current course name contains "Nonfiction", add it to the nf list
        if (course.contains("Nonfiction")) {
          this.nfCourseList += "'" + course + "'" + ", ";
        }
        
        if (course.contains("ELA")) {
          this.elaCourseList += "'" + course + "'" + ", ";
        }
        
        if (course.contains("Math")) {
          this.mathCourseList += "'" + course + "'" + ", ";
        }
        
        if (course.contains("Science")) {
          this.sciCourseList += "'" + course + "'" + ", ";
        }
        
        if (course.contains("Social")) {
          this.ssCourseList += "'" + course + "'" + ", ";
        }
        
    } //end while loop
    
    scanner.close();
    
    //add a ) to the end of the String
    this.nfCourseList += ")";
    this.elaCourseList += ")";
    this.mathCourseList += ")";
    this.sciCourseList += ")";
    this.ssCourseList += ")";
    
    //replace the final trailing comma and space at the end of the String
    this.nfCourseList = this.nfCourseList.replace(", )", ")");
    this.elaCourseList = this.elaCourseList.replace(", )", ")");
    this.mathCourseList = this.mathCourseList.replace(", )", ")");
    this.sciCourseList = this.sciCourseList.replace(", )", ")");
    this.ssCourseList = this.ssCourseList.replace(", )", ")");

  } //end getCourseList method
  
  //method to get the String message that should go above the chart in an email
  public String getEmailMessage() {
    
    String message = "<strong>This chart shows the average number of <u>teacher-created</u> assessment results " + 
      "recorded per student in each core-content subject during the given date range.</strong><br><br>.";
    
    return message;
  }
  
  //method to populate the String list of term_bin_ids which will be appended to the Assessment-Students API endpoint
  public void getTermBinIDs() {
  
    Connection c = null;
    Statement stmt = null;
    
    try {
      
      Class.forName("org.sqlite.JDBC");
      c = DriverManager.getConnection("jdbc:sqlite:" + this.dbName);
      c.setAutoCommit(false);
      System.out.println("opened database successfully");
      
      String query = (
        "SELECT " +
            "term_bin_id " +
            
        "FROM term_bins " +
        "WHERE term_bins.start_date = '" + this.termBinStartDate + "'; ");

      stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      System.out.println("SQLite query complete");
      
      this.termBinsIDList = "";
      
      //loop throw the SQL resultset
      while (rs.next()) {
          
        //add each term_bin_id to the termBinsIDList
        this.termBinsIDList += rs.getString(1);
        this.termBinsIDList += ",";
      }
      
      //remove the very last comma
      this.termBinsIDList = this.termBinsIDList.substring(0, this.termBinsIDList.length() - 1);
      
      rs.close();
      stmt.close();
      c.close();
      
      System.out.println("finished getting termbin IDs: " + this.termBinsIDList + "\n");
      
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
    }
    
  } //end getTermBinIDs method
  
} //end class
