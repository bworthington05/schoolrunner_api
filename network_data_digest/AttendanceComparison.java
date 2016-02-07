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
import org.jfree.chart.ChartColor;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.axis.NumberAxis;
import java.text.NumberFormat;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.renderer.category.StandardBarPainter;
import java.awt.Color;

//summarizes attendance data for network data digest, generates a .csv and jpeg bar graph
public class AttendanceComparison {
  
  //variable for the min_date Absences endpoint URL parameter (yyyy-MM-dd)
  private String minDate;
  private Date minDateDate;
  
  private String today;
  
  //variable for the database file path
  private String dbName;
  
  //constructor that requires a value for the min_date parameter and database file path
  public AttendanceComparison(String minDate, String dbName) {
    this.minDate = minDate;
    this.dbName = dbName;
  }
  
  public void run() {
    
    //get today's date so that it can be added to the output fileName and used as the max_date parameter
    Date todaysDate = Calendar.getInstance().getTime();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    today = simpleDateFormat.format(todaysDate);
    
    //turn the minDate string into an actual Date object
    try {
      minDateDate = simpleDateFormat.parse(minDate);
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    
    //set up a pretty date format MM/dd/yyyy that can be included in emails or graph titles
    SimpleDateFormat prettyDateFormat = new SimpleDateFormat("MM/dd/yyyy");
    
    //make strings for today's date and the minDate using the pretty date format
    String prettyToday = prettyDateFormat.format(todaysDate);
    String prettyMinDate = prettyDateFormat.format(minDateDate);
    
    //file paths for files to be saved
    String csvFileName = "/home/ubuntu/workspace/output/data_digest/Attendance_Comparison_" + today + ".csv";
    String chartFileNameString = "/home/ubuntu/workspace/output/data_digest/Attendance_Comparison_Graph_" + today + ".jpeg";
    
    //custom Absences endpoint url that includes the parameters for active=1, min_date = minDate, and max_date = today
    String absencesEndpoint = "https://renew.schoolrunner.org/api/v1/absences/?limit=30000&active=1&min_date=" + this.minDate +
      "&max_date=" + today;
    
    //custom Students endpoint url that includes the parameter active=1
    String studentsEndpoint = "https://renew.schoolrunner.org/api/v1/students/?limit=30000&active=1";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    database.createAbsencesTable(absencesEndpoint);
    database.createAbsenceTypesTable();
    database.createSchoolsTable();
    database.createStudentsTable(studentsEndpoint);
    
    Connection c = null;
    Statement stmt = null;
    
    try {
      
      Class.forName("org.sqlite.JDBC");
      c = DriverManager.getConnection("jdbc:sqlite:" + dbName);
      c.setAutoCommit(false);
      System.out.println("opened database successfully");
      
      String query = (
        "SELECT " +
            "schools.display_name, " +
            
            //for each school, get the number of of specified attendance codes recorded
            
            "SUM(CASE WHEN absence_types.absence_code = 'A' THEN 1 ELSE 0 END), " +
              
            "SUM(CASE WHEN absence_types.absence_code = 'AE' THEN 1 ELSE 0 END), " +
              
            "SUM(CASE WHEN absence_types.absence_code = 'T' THEN 1 ELSE 0 END), " +
              
            "SUM(CASE WHEN absence_types.absence_code = 'TE' THEN 1 ELSE 0 END), " +
            
            //subtract sum of tardies & absences from (active student count * # of school days in this range- i.e., "student days") 
            //to get the value for the top (filler) part of barchart
            "(active_students.count_of_students*4)-(SUM(CASE WHEN absence_types.absence_code IN ('A','AE','T','TE') THEN 1 ELSE 0 END))" +
            
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
	    writer.append("Everyone Else");
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
        dataset.addValue(double2, "Absent", rs.getString(1));
        dataset.addValue(double3, "Absent Excused", rs.getString(1));
        dataset.addValue(double4, "Tardy", rs.getString(1));
        dataset.addValue(double5, "Tardy Excused", rs.getString(1));
        
        //last value is student days - tardies & absences
        //which is the remaining top part of the stacked barchart (everyone else who wasnt absent or tardy)
        dataset.addValue(double6, "Everyone Else", rs.getString(1));
      }
      
      //make the stacked barchart
      JFreeChart chart = ChartFactory.createStackedBarChart(
        "Attendance Comparison: " + prettyMinDate + " - " + prettyToday, //graph title
        "Small School", //legend label
        "Avg. % of Students per Day", //vertical axis label 
        dataset, //dataset being used
        PlotOrientation.VERTICAL, //bar orientation
        true, //include legend
        true, //generate tooltips
        false); //generate URLs
      
      //absent color = red
      ChartColor absentColor = new ChartColor(252, 28, 3);
      
      //absent excused color = orange
      ChartColor absentExcusedColor = new ChartColor(253, 165, 2);
      
      //tardy color = blue
      ChartColor tardyColor = new ChartColor(40, 85, 219);
      
      //tardy excused color = purplish
      ChartColor tardyExcusedColor = new ChartColor(188, 101, 209);
      
      //everyone else color = greenish
      ChartColor everyoneElseColor = new ChartColor(113, 196, 128);
      
      //create renderer to customize the chart
      CategoryPlot plot = chart.getCategoryPlot();
      StackedBarRenderer renderer = (StackedBarRenderer) plot.getRenderer();
      renderer.setBarPainter(new StandardBarPainter());
      
      //make the dashed lines that go across the chart black
      plot.setRangeGridlinePaint(Color.BLACK);
      
      renderer.setSeriesPaint(0, absentColor);
      renderer.setSeriesPaint(1, absentExcusedColor);
      renderer.setSeriesPaint(2, tardyColor);
      renderer.setSeriesPaint(3, tardyExcusedColor);
      renderer.setSeriesPaint(4, everyoneElseColor);
      
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
         
      int width = 768; //width of the image
      int height = 576; //height of the image
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
    
  } //end run method
  
} //end class
