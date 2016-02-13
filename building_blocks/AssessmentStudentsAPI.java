import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from asessment-students API endpoint
public class AssessmentStudentsAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public AssessmentStudentsAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url, includes extra parameters for active and min_date
      this.endpoint = "https://renew.schoolrunner.org/api/v1/assessment-students/?limit=30000&active=1";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public AssessmentStudentsAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }
   
   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("assessment_students");
      
      Connection c = null;
      PreparedStatement stmt = null;
	
	   try {
         
         //connect to local SR database
         Class.forName("org.sqlite.JDBC");
         c = DriverManager.getConnection("jdbc:sqlite:" + dbName);
         c.setAutoCommit(false);
         System.out.println("opened database successfully");
         
         //loop through the JSON array and save each record to the appropriate table in the SR database
         for (int i = 0; i < innerJsonArray.size(); i++) {
            JSONObject innerJsonObj = (JSONObject) innerJsonArray.get(i);
            
            //save data from innerJsonOnj to temporary String variables that will be use in a SQL insert statement
            String assessmentStudentID = ((String) innerJsonObj.get("assessment_student_id"));
            String assessmentID = ((String) innerJsonObj.get("assessment_id"));
            String SRStudentID = ((String) innerJsonObj.get("sr_student_id"));
            String avgScore = ((String) innerJsonObj.get("avg_score"));
            String scoreOverride = ((String) innerJsonObj.get("score_override"));
            String gradeBookScore = ((String) innerJsonObj.get("grade_book_score"));
            String missing = ((String) innerJsonObj.get("missing"));
            String fromDate = ((String) innerJsonObj.get("from_date"));
            String active = ((String) innerJsonObj.get("active"));
            
            //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
            stmt = c.prepareStatement("INSERT INTO " + tableName +
               " (ASSESSMENT_STUDENT_ID, ASSESSMENT_ID, SR_STUDENT_ID, AVG_SCORE, SCORE_OVERRIDE, " +
               "GRADE_BOOK_SCORE, MISSING, FROM_DATE, ACTIVE) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
               
            stmt.setString(1, assessmentStudentID);
            stmt.setString(2, assessmentID);
            stmt.setString(3, SRStudentID);
            stmt.setString(4, avgScore);
            stmt.setString(5, scoreOverride);
            stmt.setString(6, gradeBookScore);
            stmt.setString(7, missing);
            stmt.setString(8, fromDate);
            stmt.setString(9, active);
            
            //execute the insert statement
            stmt.executeUpdate();
            
            recordsProcessed++;
         }
      
         stmt.close();
         c.commit();
         c.close();
	         
	   } catch (Exception e) {
	         System.err.println(e.getClass().getName() + ": " + e.getMessage());
	   }
	   
	   //print confirmation message
	   System.out.printf("%s%s%d%n%n", tableName, " data processing complete, total number of records: ", recordsProcessed);
		
   } //end run method
   
} //end class