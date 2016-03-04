import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from absence-types API endpoint
public class AbsenceTypesAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public AbsenceTypesAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url
      this.endpoint = "https://renew.schoolrunner.org/api/v1/absence-types/?limit=30000";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public AbsenceTypesAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }

   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("absence_types");
      
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
            String absenceTypeID = ((String) innerJsonObj.get("absence_type_id"));
            String absenceName = ((String) innerJsonObj.get("name"));
            String absenceCode = ((String) innerJsonObj.get("code"));
            String inSchool = ((String) innerJsonObj.get("in_school"));
            String displayName = ((String) innerJsonObj.get("display_name"));
            
            //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
            stmt = c.prepareStatement("INSERT INTO " + tableName +
               " (ABSENCE_TYPE_ID, ABSENCE_NAME, ABSENCE_CODE, IN_SCHOOL, DISPLAY_NAME) " +
               "VALUES (?, ?, ?, ?, ?)");
               
            stmt.setString(1, absenceTypeID);
            stmt.setString(2, absenceName);
            stmt.setString(3, absenceCode);
            stmt.setString(4, inSchool);
            stmt.setString(5, displayName);
            
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