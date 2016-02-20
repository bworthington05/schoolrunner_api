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

//calculates the breakdown of course grades for each core content course
//at each school, generates a .csv and jpeg bar graph
public class CourseGradesComparison {
  
  //variables for date formatting
  private String today;
  private Date todaysDate;
  private String prettyToday;
  
  private SimpleDateFormat simpleDateFormat;
  private SimpleDateFormat prettyDateFormat;
  
  //variable for the database file path
  private String dbName;
  
  //the actual DatabaseSetup object that creates the database and manages the API connections
  private DatabaseSetup database;
  
  //variables for storing String list of core content course names
  //put NF, Science, and SS together
  private String nfSciSSCourseList;
  private String elaCourseList;
  private String mathCourseList;
  
  //this array is used to append subject names to the JPEG and CSV file outputs
  //this order is super important- maintain this order for courseNames[] and chartFileNames[]
  private String[] subjectNames = {"NF_Sci_SS", "ELA", "Math"};
  
  //array of course names list, the SQL query and chart maker will loop through 
  //this array to make separate graphs for each subject
  private String[] courseNames = new String[3];
  
  //this array will hold the file paths for each of the JPEG charts saved
  //the getChartFile() method will return the file name String at a given index
  //getChartFile(0) = chartFileNames[0] = NF_sci_SS
  private String[] chartFileNames = new String[3];
  
  //holds the file names for only the current subject in the loop
  private String csvFileName;
  private String chartFileNameString;
  
  private Connection c;
  private Statement stmt;
  
  //variable used in SQL query to get grades for only a specific term_bin (quarter)
  //this works for all schools as long as every school's quarters have the same start date
  private String termBinStartDate;
  
  //file that holds a list of all the core content course names (required for SQL query)
  private String courseListFile = "/home/ubuntu/workspace/my_github/schoolrunner_api/network_data_digest/import_files/core_content_courses.txt";
  
  //the "order by" for SQL query (also the order of the bars in the chart)
  private String SQLiteOrderBy;  
  
  //constructor that requires the database and termBinStartDate ("yyyy-MM-dd") then takes care of some formatting stuff for today's date
  //also requires a String "order by" statement for the SQL query
  public CourseGradesComparison(DatabaseSetup database, String termBinStartDate, String SQLiteOrderBy) {
    this.database = database;
    this.dbName = this.database.getDatabaseName();
    
    this.termBinStartDate = termBinStartDate;

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
  
  //this method executes a loop for each subject- it does a SQL query to get course grades for that subject
  //saves a CSV and JPEG chart for that subject and records the file name for the chart in the chartFileNames array
  //for the DataDigest driver class, call run(), then call the getChartFile(i) method to get a given chart file path from chartFileNames
  public void run() {
    
    try {
      //get list of core content courses for each subject
      getCourseList();
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    
    this.courseNames[0] = this.nfSciSSCourseList;
    this.courseNames[1] = this.elaCourseList;
    this.courseNames[2] = this.mathCourseList;
    
    //use default endpoint URLs for all of these
    this.database.createCourseGradesTable();
    this.database.createCoursesTable();
    this.database.createSchoolsTable();
    this.database.createTermBinsTable();
    
    for(int n = 0; n < courseNames.length; n++) {
      
      System.out.println("getting grades for " + this.subjectNames[n]);
    
      //file paths for files to be saved
      this.csvFileName = "/home/ubuntu/workspace/output/data_digest/Course_Grades_Comparison_" + this.subjectNames[n] + "_" +
        this.today + ".csv";
        
      this.chartFileNameString = "/home/ubuntu/workspace/output/data_digest/Course_Grades_Comparison_Graph_" + this.subjectNames[n] + "_" +
        this.today + ".jpeg";
      
      try {
        
        Class.forName("org.sqlite.JDBC");
        c = DriverManager.getConnection("jdbc:sqlite:" + this.dbName);
        c.setAutoCommit(false);
        System.out.println("opened database successfully");
        
        String query = (
          "SELECT " +
              "replace(schools.display_name, 'Pre-K', 'PK'), " +
              
              //for each school, get the number of course grades at a specific level
              //and divide by the total number of course grades to get the relative percent
              
              "(CAST(IFNULL(i.grade_count, '0') AS FLOAT)/total_grades.grade_count), " +
  
              "(CAST(IFNULL(ii.grade_count, '0') AS FLOAT)/total_grades.grade_count), " +
              
              "(CAST(IFNULL(c.grade_count, '0') AS FLOAT)/total_grades.grade_count), " +
              
              "(CAST(IFNULL(b.grade_count, '0') AS FLOAT)/total_grades.grade_count), " +
              
              "(CAST(IFNULL(a.grade_count, '0') AS FLOAT)/total_grades.grade_count) " +
          
          "FROM courses " +
              "LEFT JOIN course_grades ON courses.course_id = course_grades.course_id " +
              "LEFT JOIN schools ON courses.sr_school_id = schools.sr_school_id " +
              "LEFT JOIN term_bins ON course_grades.term_bin_id = term_bins.term_bin_id " +
  
              //sub-query to get # of grades 0 <= i < 40
              "LEFT JOIN ( " +
                  "SELECT " +
                      "courses.sr_school_id, " +
                      "COUNT(courses.sr_school_id) as grade_count " +
                  "FROM courses " +
                      "LEFT JOIN course_grades ON courses.course_id = course_grades.course_id " +
                      "LEFT JOIN schools ON courses.sr_school_id = schools.sr_school_id " +
                      "LEFT JOIN term_bins ON course_grades.term_bin_id = term_bins.term_bin_id " +
                  "WHERE course_grades.active = '1' AND courses.course_name IN " + this.courseNames[n] + " " +
                  "AND term_bins.start_date = '" + this.termBinStartDate + "' " +
                  "AND (CAST(course_grades.score as FLOAT)) >= 0 " +
                  "AND (CAST(course_grades.score as FLOAT)) < 40 " +
                  "GROUP BY courses.sr_school_id " +
              ") i ON courses.sr_school_id = i.sr_school_id " +
              
              //sub-query to get # of grades 40 <= ii < 50
              "LEFT JOIN ( " +
                  "SELECT " +
                      "courses.sr_school_id, " +
                      "COUNT(courses.sr_school_id) as grade_count " +
                  "FROM courses " +
                      "LEFT JOIN course_grades ON courses.course_id = course_grades.course_id " +
                      "LEFT JOIN schools ON courses.sr_school_id = schools.sr_school_id " +
                      "LEFT JOIN term_bins ON course_grades.term_bin_id = term_bins.term_bin_id " +
                  "WHERE course_grades.active = '1' AND courses.course_name IN " + this.courseNames[n] + " " +
                  "AND term_bins.start_date = '" + this.termBinStartDate + "' " +
                  "AND (CAST(course_grades.score as FLOAT)) >= 40 " +
                  "AND (CAST(course_grades.score as FLOAT)) < 50 " +
                  "GROUP BY courses.sr_school_id " +
              ") ii ON courses.sr_school_id = ii.sr_school_id " +
              
              //sub-query to get # of grades 50 <= c < 75
              "LEFT JOIN ( " +
                  "SELECT " +
                      "courses.sr_school_id, " +
                      "COUNT(courses.sr_school_id) as grade_count " +
                  "FROM courses " +
                      "LEFT JOIN course_grades ON courses.course_id = course_grades.course_id " +
                      "LEFT JOIN schools ON courses.sr_school_id = schools.sr_school_id " +
                      "LEFT JOIN term_bins ON course_grades.term_bin_id = term_bins.term_bin_id " +
                  "WHERE course_grades.active = '1' AND courses.course_name IN " + this.courseNames[n] + " " +
                  "AND term_bins.start_date = '" + this.termBinStartDate + "' " +
                  "AND (CAST(course_grades.score as FLOAT)) >= 50 " +
                  "AND (CAST(course_grades.score as FLOAT)) < 75 " +
                  "GROUP BY courses.sr_school_id " +
              ") c ON courses.sr_school_id = c.sr_school_id " +
              
              //sub-query to get # of grades 75 <= b < 90
              "LEFT JOIN ( " +
                  "SELECT " +
                      "courses.sr_school_id, " +
                      "COUNT(courses.sr_school_id) as grade_count " +
                  "FROM courses " +
                      "LEFT JOIN course_grades ON courses.course_id = course_grades.course_id " +
                      "LEFT JOIN schools ON courses.sr_school_id = schools.sr_school_id " +
                      "LEFT JOIN term_bins ON course_grades.term_bin_id = term_bins.term_bin_id " +
                  "WHERE course_grades.active = '1' AND courses.course_name IN " + this.courseNames[n] + " " +
                  "AND term_bins.start_date = '" + this.termBinStartDate + "' " +
                  "AND (CAST(course_grades.score as FLOAT)) >= 75 " +
                  "AND (CAST(course_grades.score as FLOAT)) < 90 " +
                  "GROUP BY courses.sr_school_id " +
              ") b ON courses.sr_school_id = b.sr_school_id " +
              
              //sub-query to get # of grades 90 <= a < 200
              "LEFT JOIN ( " +
                  "SELECT " +
                      "courses.sr_school_id, " +
                      "COUNT(courses.sr_school_id) as grade_count " +
                  "FROM courses " +
                      "LEFT JOIN course_grades ON courses.course_id = course_grades.course_id " +
                      "LEFT JOIN schools ON courses.sr_school_id = schools.sr_school_id " +
                      "LEFT JOIN term_bins ON course_grades.term_bin_id = term_bins.term_bin_id " +
                  "WHERE course_grades.active = '1' AND courses.course_name IN " + this.courseNames[n] + " " +
                  "AND term_bins.start_date = '" + this.termBinStartDate + "' " +
                  "AND (CAST(course_grades.score as FLOAT)) >= 90 " +
                  "AND (CAST(course_grades.score as FLOAT)) < 200 " +
                  "GROUP BY courses.sr_school_id " +
              ") a ON courses.sr_school_id = a.sr_school_id " +
              
              //sub-query to get total number of grades
              "LEFT JOIN ( " +
                  "SELECT " +
                      "courses.sr_school_id, " +
                      "COUNT(courses.sr_school_id) as grade_count " +
                  "FROM courses " +
                      "LEFT JOIN course_grades ON courses.course_id = course_grades.course_id " +
                      "LEFT JOIN schools ON courses.sr_school_id = schools.sr_school_id " +
                      "LEFT JOIN term_bins ON course_grades.term_bin_id = term_bins.term_bin_id " +
                  "WHERE course_grades.active = '1' AND courses.course_name IN " + this.courseNames[n] + " " +
                  "AND term_bins.start_date = '" + this.termBinStartDate + "' " +
                  "AND (CAST(course_grades.score as FLOAT)) >= 0 " +
                  "AND (CAST(course_grades.score as FLOAT)) < 200 " +
                  "GROUP BY courses.sr_school_id " +
              ") total_grades ON courses.sr_school_id = total_grades.sr_school_id " +
          
          //don't count RAHS, ECC, or RSP
          "WHERE course_grades.active = '1' AND courses.sr_school_id NOT IN ('5','17','18') " +
          "AND term_bins.start_date = '" + this.termBinStartDate + "' " +
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
  	    writer.append("# of 0-39% Grades");
  	    writer.append(',');
  	    writer.append("# of 40-49% Grades");
  	    writer.append(',');
  	    writer.append("# of 50-74% Grades");
  	    writer.append(',');
  	    writer.append("# of 75-89% Grades");
  	    writer.append(',');
  	    writer.append("# of 90% + Grades");
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
          dataset.addValue(double2, "Grade 0-39 (I-)", rs.getString(1));
          dataset.addValue(double3, "40-49 (I)", rs.getString(1));
          dataset.addValue(double4, "50-74 (C)", rs.getString(1));
          dataset.addValue(double5, "75-89 (B)", rs.getString(1));
          dataset.addValue(double6, "90+ (A)", rs.getString(1));
        }
        
        //make the stacked barchart
        JFreeChart chart = ChartFactory.createStackedBarChart(
          null, //graph title is null (will be set later)
          "Small School", //legend label
          "% of Students", //vertical axis label 
          dataset, //dataset being used
          PlotOrientation.VERTICAL, //bar orientation
          true, //include legend
          true, //generate tooltips
          false); //generate URLs
          
        //title of the chart
        String titleString = this.subjectNames[n] + " Course Grades Comparison\n as of " + this.prettyToday;
        
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
        renderer.setSeriesPaint(0, MyColors.RED);
        renderer.setSeriesPaint(1, MyColors.ORANGE);
        renderer.setSeriesPaint(2, MyColors.YELLOW);
        renderer.setSeriesPaint(3, MyColors.LIGHT_GREEN);
        renderer.setSeriesPaint(4, MyColors.GREEN);
        
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
      
      //add the current chartFileNameString (for the current subject) to the chartFileNames array
      this.chartFileNames[n] = chartFileNameString;      
      
    } //end for loop
    
  } //end run method
  
  //method that makes separate lists of core content course names for each subject
  //lists are fomatted at "(a, b, c)" so that they can be used in SQL query
  private void getCourseList() throws FileNotFoundException {
  
    this.nfSciSSCourseList = "(";
    this.elaCourseList = "(";
    this.mathCourseList = "(";
    
    //read the file that contains the list of core content course names
    Scanner scanner = new Scanner(new File(this.courseListFile));
    
    //use hasNextLine() and nextLine() so that the whole course name is returned since there are spaces in the name
    while(scanner.hasNextLine()) {
        
        String course = scanner.nextLine();
        
        //if the current course name contains "Nonfiction", add it to the nf list
        if (course.contains("Nonfiction")) {
          this.nfSciSSCourseList += "'" + course + "'" + ", ";
        }
        
        if (course.contains("ELA")) {
          this.elaCourseList += "'" + course + "'" + ", ";
        }
        
        if (course.contains("Math")) {
          this.mathCourseList += "'" + course + "'" + ", ";
        }
        
        if (course.contains("Science")) {
          this.nfSciSSCourseList += "'" + course + "'" + ", ";
        }
        
        if (course.contains("Social")) {
          this.nfSciSSCourseList += "'" + course + "'" + ", ";
        }
        
    } //end while loop
    
    scanner.close();
    
    //add a ) to the end of the String
    this.nfSciSSCourseList += ")";
    this.elaCourseList += ")";
    this.mathCourseList += ")";
    
    //replace the final trailing comma and space at the end of the String
    this.nfSciSSCourseList = this.nfSciSSCourseList.replace(", )", ")");
    this.elaCourseList = this.elaCourseList.replace(", )", ")");
    this.mathCourseList = this.mathCourseList.replace(", )", ")");

  } //end getCourseList method
  
  //method to get the String message that should go above the chart in an email
  public String getEmailMessage() {
    
    String message = "<strong>These charts show the percent of students whose current grade in each core content " +
      "subject is at a certain level.  Grades are grouped by percents that correspond to the standard ReNEW grading scale. " +
      "Nonfiction, Science, and Social Studies are combined.</strong><br><br>.";
    
    return message;
  }
  
  public String getChartFile(int i) {
    
    //get the file path for a specific subject's JPEG chart (NFSciSS, ELA, Math)
    return chartFileNames[i];
  }
  
  public String getAnalysisLinks() {
    
    String rcaa34link = "https://renew.schoolrunner.org/analysis/?analysis_report_id=745";
    
    String links = "<strong>For a more detailed breakdown of course grades by course and student, " +
                    "take a look at your school's course grades report in Schoolrunner:</strong><br>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>RCAA PK-2</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>RCAA 3-4</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>RCAA 5-8</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>STA PK-2</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>STA 3-8</font size='3'></strong></a><br>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>DTA PK-2</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>DTA 3-5</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>DTA 6-8</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>SCH PK-3</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>SCH 4-5</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>SCH 6-8</font size='3'></strong></a><br>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>MCPA PK-4</font size='3'></strong></a>" +
                    "&emsp;<a href='" + rcaa34link + "'><strong><font size='3'>MCPA 5-8</font size='3'></strong></a><br><br>";
    return links;
  }
  
} //end class
