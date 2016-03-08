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

//generates a .CSV file that shows each student's total minutes out of class as recorded through
//Schoolrunner's Incidents page
public class MinutesOutOfClass {
  
  //today's date used in graph title and .CSV filename
  private String today;
  private Date todaysDate;
  
  private String minDate;
  
  private SimpleDateFormat simpleDateFormat;
  
  //variable for the database file path
  private String dbName;
  
  //the actual DatabaseSetup object that creates the database and manages the API connections
  private DatabaseSetup database;
  
  //the "order by" for SQL query (also the order of the bars in the chart)
  private String SQLiteOrderBy;
  
  //constructor that requires the database and min_date (yyyy-MM-dd) for API parameters
  //also requires a String "order by" statement for the SQL query
  public MinutesOutOfClass(DatabaseSetup database, String minDate, String SQLiteOrderBy) {
    this.database = database;
    this.minDate = minDate;
    this.dbName = this.database.getDatabaseName();

    this.simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    //get today's date so that it can be added to file names and graphs
    this.todaysDate = Calendar.getInstance().getTime();
    this.simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    this.today = simpleDateFormat.format(this.todaysDate);
    
    this.SQLiteOrderBy = SQLiteOrderBy;
  } //end constructor
  
  //method that gets the total minutes out of class for each student and saves a .CSV file of the data
  public String run() {
    
    //file paths for files to be saved
    String csvFileName = "/home/ubuntu/workspace/output/min_out_of_class/Minutes_Out_Of_Class_" + this.today + ".csv";
    
    //custom Incidents-Students endpoint url (this is where we get the minutes of out class for each incident)
    String incidentStudentsEndpoint = "https://renew.schoolrunner.org/api/v1/incidents?limit=30000&active=1&with_related=true&min_date=" + this.minDate;
    
    //custom Incidents endpoint url
    String incidentsEndpoint = "https://renew.schoolrunner.org/api/v1/incidents?limit=30000&active=1&min_date=" + this.minDate;
    
    //custom Students endpoint url- make sure to include inactive students incase some active incidents involed students who have transferred out
    String studentsEndpoint = "https://renew.schoolrunner.org/api/v1/students/?limit=30000";
    
    this.database.createIncidentStudentsTable(incidentStudentsEndpoint);
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
            "students.ps_student_number, " +
            "students.last_name, " +
            "students.first_name, " +
            
            //if the total is 0, then use null
            //if null, then replace with message
            "IFNULL((CASE WHEN min_out_of_class.total IS 0 THEN NULL ELSE min_out_of_class.total END), 'No minutes or consequence recorded'), " +

            //divide total minutes out of class by 360 to get approximate number of equivalent school days
            "ROUND((CAST(min_out_of_class.total AS FLOAT) / 360), 1) " +
                
        "FROM incident_students " +
            "LEFT OUTER JOIN incidents ON incident_students.incident_id = incidents.incident_id " +
            "LEFT OUTER JOIN students ON incident_students.sr_student_id = students.sr_student_id " +
            "LEFT OUTER JOIN schools ON incidents.sr_school_id = schools.sr_school_id " +
            
              //sub-query to get total # of minutes out of class for each student who has at least some minutes recorded
              "LEFT OUTER JOIN ( " +
                  "SELECT " +
                      "incident_students.sr_student_id, " +
                      "SUM(incident_students.minutes_out_of_class) as total " +
                  "FROM incident_students " +
                      "LEFT OUTER JOIN incidents ON incident_students.incident_id = incidents.incident_id " +
                      "LEFT JOIN students ON incident_students.sr_student_id = students.sr_student_id " +
                      
                  //make sure BOTH the actual incident and incident_student record are active
                  "WHERE incidents.active = '1' AND incident_students.active = '1'" +
                  "GROUP BY students.sr_student_id " +
              ") min_out_of_class ON incident_students.sr_student_id = min_out_of_class.sr_student_id " +

        "WHERE incidents.active = '1' AND incident_students.active = '1'" +
        "GROUP BY students.sr_student_id " +
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
	    writer.append("PS Student_Number");
	    writer.append(',');
	    writer.append("Last Name");
	    writer.append(',');
	    writer.append("First Name");
	    writer.append(',');
	    writer.append("Total Minutes Out of Class");
	    writer.append(',');
	    writer.append("Approx. Equiv. School Days");
	    writer.append('\n');
      
      //loop throw the SQL resultset and save records to the filewriter
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
      }
      
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
    
    return csvFileName;
    
  } //end run method
  
} //end class
