import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from course grades API endpoint
public class CourseGradesAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public CourseGradesAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url
      this.endpoint = "https://renew.schoolrunner.org/api/v1/course_grades/?limit=30000&active=1";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public CourseGradesAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }

   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("course_grades");
      
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
            String courseGradeID = ((String) innerJsonObj.get("course_grade_id"));
            String courseID = ((String) innerJsonObj.get("course_id"));
            String SRStudentID = ((String) innerJsonObj.get("student_id"));
            String termBinID = ((String) innerJsonObj.get("term_bin_id"));
            String score = ((String) innerJsonObj.get("score"));
            String scoreOverride = ((String) innerJsonObj.get("score_override"));
            String gradingScaleLevelID = ((String) innerJsonObj.get("grading_scale_level_id"));
            String asOf = ((String) innerJsonObj.get("as_of"));
            String active = ((String) innerJsonObj.get("active"));
            
            //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
            stmt = c.prepareStatement("INSERT INTO " + tableName +
               " (COURSE_GRADE_ID, COURSE_ID, SR_STUDENT_ID, TERM_BIN_ID, SCORE, SCORE_OVERRIDE, GRADING_SCALE_LEVEL_ID, AS_OF, ACTIVE) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            stmt.setString(1, courseGradeID);   
            stmt.setString(2, courseID);
            stmt.setString(3, SRStudentID);
            stmt.setString(4, termBinID);
            stmt.setString(5, score);
            stmt.setString(6, scoreOverride);
            stmt.setString(7, gradingScaleLevelID);
            stmt.setString(8, asOf);
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