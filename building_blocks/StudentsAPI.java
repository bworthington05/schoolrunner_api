import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from students API endpoint
public class StudentsAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public StudentsAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url
      this.endpoint = "https://renew.schoolrunner.org/api/v1/students/?limit=30000";
   }
   
   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public StudentsAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }
   
   private int activeStudentCount;

   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("students");
      
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
            String firstName = ((String) innerJsonObj.get("first_name"));
            String lastName = ((String) innerJsonObj.get("last_name"));
            String SRSchoolID = ((String) innerJsonObj.get("school_id"));
            String gradeLevelID = ((String) innerJsonObj.get("grade_level_id"));
            String SRStudentID = ((String) innerJsonObj.get("student_id"));
            String PSStudentNumber = ((String) innerJsonObj.get("sis_id"));
            String PSStudentID = ((String) innerJsonObj.get("external_id"));
            String UID = ((String) innerJsonObj.get("state_id"));
            String active = ((String) innerJsonObj.get("active"));
            
            //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
            stmt = c.prepareStatement("INSERT INTO " + tableName +
               " (FIRST_NAME, LAST_NAME, SR_SCHOOL_ID, GRADE_LEVEL_ID, SR_STUDENT_ID, PS_STUDENT_NUMBER, PS_STUDENT_ID, UID, ACTIVE) " +
               "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, SRSchoolID);
            stmt.setString(4, gradeLevelID);
            stmt.setString(5, SRStudentID);
            stmt.setString(6, PSStudentNumber);
            stmt.setString(7, PSStudentID);
            stmt.setString(8, UID);
            stmt.setString(9, active);
            
            //execute the insert statement
            stmt.executeUpdate();
            
            recordsProcessed++;
            
            //if active status of student is "1" (active), increment active student counter
            if ("1".equals((String) innerJsonObj.get("active"))) {
               activeStudentCount++;
            }
         }
         
         stmt.close();
         c.commit();
         c.close();
	         
	   } catch (Exception e) {
	         System.err.println(e.getClass().getName() + ": " + e.getMessage());
	   }
	   
	   //print confirmation message
	   System.out.printf("%s%s%d%n%s%d%n%n", tableName, " data processing complete, total number of records: ", recordsProcessed,
	      "number of active students processed: ", activeStudentCount);
		
   } //end run method
   
} //end class