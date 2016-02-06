import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from absences API endpoint
public class AbsencesAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public AbsencesAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url, includes extra parameter for min_date
      this.endpoint = "https://renew.schoolrunner.org/api/v1/absences/?limit=30000&min_date=2015-07-22";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public AbsencesAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }
   
   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("absences");
      
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
            
            //save data from innerJsonObj to temporary String variables that will be use in a SQL insert statement
            String absenceID = ((String) innerJsonObj.get("absence_id"));
            String SRSchoolID = ((String) innerJsonObj.get("school_id"));
            String SRStudentID = ((String) innerJsonObj.get("student_id"));
            String SRStaffMemberID = ((String) innerJsonObj.get("staff_member_id"));
            String absenceDate = ((String) innerJsonObj.get("date"));
            String absenceTypeID = ((String) innerJsonObj.get("absence_type_id"));
            String active = ((String) innerJsonObj.get("active"));
            
            //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
            stmt = c.prepareStatement("INSERT INTO " + tableName +
               " (ABSENCE_ID, SR_SCHOOL_ID, SR_STUDENT_ID, SR_STAFF_MEMBER_ID, ABSENCE_DATE, ABSENCE_TYPE_ID, ACTIVE) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?)");
               
            stmt.setString(1, absenceID);
            stmt.setString(2, SRSchoolID);
            stmt.setString(3, SRStudentID);
            stmt.setString(4, SRStaffMemberID);
            stmt.setString(5, absenceDate);
            stmt.setString(6, absenceTypeID);
            stmt.setString(7, active);
            
            //execute the insert statement
            stmt.executeUpdate();
            
            recordsProcessed++;
            
         } //end for loop
         
         c.commit();
         stmt.close();
         c.close();
	         
	   } catch (Exception e) {
	         System.err.println(e.getClass().getName() + ": " + e.getMessage());
	   }
	   
	   //print confirmation message
	   System.out.printf("%s%s%d%n%n", tableName, " data processing complete, total number of records: ", recordsProcessed);
		
   } //end run method
   
} //end class