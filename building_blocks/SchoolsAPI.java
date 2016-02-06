import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from schools API endpoint
public class SchoolsAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public SchoolsAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url
      this.endpoint = "https://renew.schoolrunner.org/api/v1/schools/?limit=30000";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public SchoolsAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }

   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("schools");
      
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
            String longName = ((String) innerJsonObj.get("long_name"));
            String SRSchoolID = ((String) innerJsonObj.get("school_id"));
            String PSSchoolID = ((String) innerJsonObj.get("external_id"));
            String displayName = ((String) innerJsonObj.get("display_name"));
            String shortName = ((String) innerJsonObj.get("short_name"));
            
            //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
            stmt = c.prepareStatement("INSERT INTO " + tableName +
               " (LONG_NAME, SR_SCHOOL_ID, PS_SCHOOL_ID, DISPLAY_NAME, SHORT_NAME) " +
               "VALUES (?, ?, ?, ?, ?)");
               
            stmt.setString(1, longName);
            stmt.setString(2, SRSchoolID);
            stmt.setString(3, PSSchoolID);
            stmt.setString(4, displayName);
            stmt.setString(5, shortName);
            
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