import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from staff-members API endpoint
public class StaffAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public StaffAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url
      this.endpoint = "https://renew.schoolrunner.org/api/v1/staff-members/?limit=30000";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public StaffAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }

   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("staff_members");
      
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
            String SRStaffMemberID = ((String) innerJsonObj.get("staff_member_id"));
            String SRUserID = ((String) innerJsonObj.get("user_id"));
            String firstName = ((String) innerJsonObj.get("first_name"));
            String lastName = ((String) innerJsonObj.get("last_name"));
            String displayName = ((String) innerJsonObj.get("display_name"));
            String SRSchoolID = ((String) innerJsonObj.get("school_id"));
            String PSTeacherNumber = ((String) innerJsonObj.get("sis_id"));
            String PSTeacherID = ((String) innerJsonObj.get("external_id"));
            String title = ((String) innerJsonObj.get("title"));
            String email = ((String) innerJsonObj.get("email"));
            String active = ((String) innerJsonObj.get("active"));
            
            //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
            stmt = c.prepareStatement("INSERT INTO " + tableName +
               " (SR_STAFF_MEMBER_ID, SR_USER_ID, FIRST_NAME, LAST_NAME, DISPLAY_NAME, SR_SCHOOL_ID, PS_TEACHER_NUMBER, PS_TEACHER_ID, TITLE, EMAIL, ACTIVE) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
               
            stmt.setString(1, SRStaffMemberID);
            stmt.setString(2, SRUserID);
            stmt.setString(3, firstName);
            stmt.setString(4, lastName);
            stmt.setString(5, displayName);
            stmt.setString(6, SRSchoolID);
            stmt.setString(7, PSTeacherNumber);
            stmt.setString(8, PSTeacherID);
            stmt.setString(9, title);
            stmt.setString(10, email);
            stmt.setString(11, active);
            
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