import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from term bin API endpoint
public class TermBinsAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public TermBinsAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url
      this.endpoint = "https://renew.schoolrunner.org/api/v1/term-bins/?limit=30000&active=1";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public TermBinsAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }

   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("term_bins");
      
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
            String termBinID = ((String) innerJsonObj.get("term_bin_id"));
            String termID = ((String) innerJsonObj.get("term_id"));
            String SRSchoolID = ((String) innerJsonObj.get("school_id"));
            String shortName = ((String) innerJsonObj.get("short_name"));
            String longName = ((String) innerJsonObj.get("long_name"));
            String startDate = ((String) innerJsonObj.get("start_date"));
            String endDate = ((String) innerJsonObj.get("end_date"));
            String active = ((String) innerJsonObj.get("active"));
            
            //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
            stmt = c.prepareStatement("INSERT INTO " + tableName +
               " (TERM_BIN_ID, TERM_ID, SR_SCHOOL_ID, SHORT_NAME, LONG_NAME, START_DATE, END_DATE, ACTIVE) " +
               "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
               
            stmt.setString(1, termBinID);
            stmt.setString(2, termID);
            stmt.setString(3, SRSchoolID);
            stmt.setString(4, shortName);
            stmt.setString(5, longName);
            stmt.setString(6, startDate);
            stmt.setString(7, endDate);
            stmt.setString(8, active);
            
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