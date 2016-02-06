import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

//returns a list of assessments not aligned to objectives
public class AssessmentsWithoutObjectives {
  
  public static void main(String args[]) {
    
    //get today's date so that it can be add to fileName or used in an API endpoint parameter
    Date date = Calendar.getInstance().getTime();
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String today = simpleDateFormat.format(date);
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB.db";
    
    //file path for output .csv to be saved
    String fileName = "/home/ubuntu/workspace/output/Assessments_Without_Objectives_" + today + ".csv";
    
    //custom endpoint url that includes the parameters has_objectives=0 (missing objectives) and minimum date
    String assessmentEndpoint = "https://renew.schoolrunner.org/api/v1/assessments/?limit=30000&active=1&min_date=2016-01-06&has_objectives=0";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    database.createAssessmentsTable(assessmentEndpoint);
    database.createSchoolsTable();
    database.createCoursesTable();
    database.createStaffTable();
    
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
            "IFNULL(courses.course_name, '**NO_COURSE_SELECTED**'), " +
            "staff.display_name, " +
            "staff.email, " +
            "assessments.assessment_name, " +
            "assessments.assessment_date, " +
            "assessments.present_students, " +
            "assessments.enrolled_students, " +
            "('https://renew.schoolrunner.org/assessment/edit/' || assessments.assessment_id) " +
        "FROM assessments " +
            "LEFT OUTER JOIN courses ON assessments.course_id = courses.course_id " +
            "LEFT OUTER JOIN staff ON assessments.sr_staff_member_id = staff.sr_staff_member_id " +
            "LEFT OUTER JOIN schools ON assessments.sr_school_id = schools.sr_school_id " +
        "WHERE assessments.sr_school_id != '5' " +
            "AND assessments.present_students != '0' " +
            "AND assessments.present_students != 'null' " +
            "AND staff.email != 'null' " +
            
            //for testing purposes
            //"AND staff.email = 'bworthington@renewschools.org' " +
        "ORDER BY schools.display_name, courses.course_name, staff.display_name, assessments.assessment_date; ");

      stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      System.out.println("SQLite query complete");
      
      //arrays list that will hold assessment info and then be passed to email method
      ArrayList<String> courseName = new ArrayList<String>();
      ArrayList<String> staffEmail = new ArrayList<String>();
      ArrayList<String> assessmentName = new ArrayList<String>();
      ArrayList<String> assessmentDate = new ArrayList<String>();
      ArrayList<String> studentsAssessed = new ArrayList<String>();
      ArrayList<String> editURL = new ArrayList<String>();
      
      //new filewriter object for saving the data
      FileWriter writer = new FileWriter(fileName);
      
      writer.append("SCHOOL");
	    writer.append(',');
	    writer.append("COURSE_NAME");
	    writer.append(',');
	    writer.append("STAFF");
	    writer.append(',');
	    writer.append("STAFF_EMAIL");
	    writer.append(',');
	    writer.append("ASSESSMENT_NAME");
	    writer.append(',');
	    writer.append("ASSESSMENT_DATE");
	    writer.append(',');
	    writer.append("STUDENTS_ASSESSED");
	    writer.append(',');
	    writer.append("STUDENTS_ENROLLED");
	    writer.append(',');
	    writer.append("EDIT_URL");
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
    	    writer.append(',');
    	    writer.append("\"" + rs.getString(5) + "\"");
    	    writer.append(',');
    	    writer.append("\"" + rs.getString(6) + "\"");
    	    writer.append(',');
    	    writer.append("\"" + rs.getString(7) + "\"");
    	    writer.append(',');
    	    writer.append("\"" + rs.getString(8) + "\"");
    	    writer.append(',');
    	    writer.append("\"" + rs.getString(9) + "\"");
    	    writer.append('\n');
    	    
    	    //add certain columns of each record to the array lists that will be passed to email method
          courseName.add(rs.getString(2));
          staffEmail.add(rs.getString(4));
          assessmentName.add(rs.getString(5));
          assessmentDate.add(rs.getString(6));
          studentsAssessed.add(rs.getString(7));
          editURL.add(rs.getString(9));
      }
      
      rs.close();
      stmt.close();
      c.close();

	    writer.flush();
	    writer.close();
	    
	    //print confirmation that .csv file was saved
	    System.out.println(fileName + " saved successfully\n");
	    
	    //create object of class that handles emailing teachers who created assessments without objectives
	    //pass in the relevant array lists to the constructor
      EmailAssessmentsWithoutObjectives email = new EmailAssessmentsWithoutObjectives(
	      courseName, staffEmail, assessmentName, assessmentDate, studentsAssessed, editURL);
      
      //run method executes the email loop- will ask for email login credentials
      email.run();
      
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
    }
    
  } //end main method
  
} //end class

