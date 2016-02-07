import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

//calculates the average number of out-of-school suspension days per thousand students
public class SuspensionComparisonAcrossSchools {
  
  public static void main(String args[]) {
    
    //get today's date so that it can be added to fileName or used in an API endpoint parameter
    Date date = Calendar.getInstance().getTime();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String today = simpleDateFormat.format(date);
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB.db";
    
    //file path for output .csv to be saved
    String fileName = "/home/ubuntu/workspace/output/SuspensionComparisonAcrossSchools_" + today + ".csv";
    
    //custom endpoint url that includes the parameters for minimum date and attendance records only with SU (suspension code, absence type ID = 24)
    String absencesEndpoint = "https://renew.schoolrunner.org/api/v1/absences/?limit=30000&min_date=2015-07-22&absence_type_ids=24";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    database.createAbsencesTable(absencesEndpoint);
    database.createAbsenceTypesTable();
    database.createSchoolsTable();
    database.createStudentsTable();
    
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
            "COUNT(absence_types.absence_code), " +
            "active_students.count_of_students, " +
            
            //for each school, divide the # of suspension days by the # of active students and multiply by 1000, round the result to 0 decimal places
            "ROUND(((CAST((COUNT(absence_types.absence_code)) AS FLOAT) / active_students.count_of_students) * 1000), 0) " +
        "FROM absences " +
            "LEFT OUTER JOIN absence_types ON absences.absence_type_id = absence_types.absence_type_id " +
            "LEFT OUTER JOIN schools ON absences.sr_school_id = schools.sr_school_id " +
            "LEFT OUTER JOIN ( " +
            
            //sub-query to get # of active students in each small school
                "SELECT " +
                    "students.sr_school_id, " +
                    "COUNT(students.sr_school_id) as count_of_students " +
                "FROM students " +
                "WHERE students.active = '1' " +
                "GROUP BY students.sr_school_id " +
            ") active_students ON absences.sr_school_id = active_students.sr_school_id " +

        "WHERE absences.active = '1' " +
        "GROUP BY schools.display_name " +
        "ORDER BY schools.display_name; ");

      stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      System.out.println("SQLite query complete");
      
      //new filewriter object for saving the data
      FileWriter writer = new FileWriter(fileName);
      
      writer.append("SCHOOL");
	    writer.append(',');
	    writer.append("RAW # OF SUSPENSION ABSENCES");
	    writer.append(',');
	    writer.append("# OF ACTIVE STUDENTS");
	    writer.append(',');
	    writer.append("# OF SUSPENSION DAYS PER THOUSAND STUDENTS");
	    writer.append('\n');
      
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
    	    writer.append('\n');
      }
      
      rs.close();
      stmt.close();
      c.close();

	    writer.flush();
	    writer.close();
	    
	    //print confirmation that .csv file was saved
	    System.out.println(fileName + " saved successfully\n");
      
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
    }
    
  } //end main method
  
} //end class

