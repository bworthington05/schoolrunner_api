import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from incidents API endpoint, but specifically from the incident_suspension blocks
//"suspensions" = any incident consequence, not just OSS
//assumes regular incidents table is being created or has already been created too
public class IncidentSuspensionsAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public IncidentSuspensionsAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url, includes extra parameters for active, min_date, and with_related (to display consequence details)
      this.endpoint = "https://renew.schoolrunner.org/api/v1/incidents/?limit=30000&active=1&min_date=2015-07-22&with_related=true";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public IncidentSuspensionsAPI(String username, String password, String dbName, String tableName, String endpoint) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      this.endpoint = endpoint;
   }
   
   @Override
   public void run() {
      
      JSONArray innerJsonArray = getAllPages("incidents");
      
      Connection c = null;
      PreparedStatement stmt = null;
	
	   try {
         
         //connect to local SR database
         Class.forName("org.sqlite.JDBC");
         c = DriverManager.getConnection("jdbc:sqlite:" + dbName);
         c.setAutoCommit(false);
         System.out.println("opened database successfully");
         
         //loop through the JSON array and get the inner JSON array of suspension records within each incident
         for (int i = 0; i < innerJsonArray.size(); i++) {
            JSONObject innerJsonObj = (JSONObject) innerJsonArray.get(i);
               
            JSONArray innerInnerJsonArray = (JSONArray) innerJsonObj.get("incident_suspensions");
            
            for (int n = 0; n < innerInnerJsonArray.size(); n++) {
               
               JSONObject innerInnerJsonObj = (JSONObject) innerInnerJsonArray.get(n);
            
               //save data from innerInnerJsonObj (suspension records) to temporary String variables that will be used in a SQL insert statement
               String incidentSuspenionID = ((String) innerInnerJsonObj.get("incident_suspension_id"));
               String incidentID = ((String) innerInnerJsonObj.get("incident_id"));
               String incidentStudentID = ((String) innerInnerJsonObj.get("incident_student_id"));
               String SRStudentID = ((String) innerInnerJsonObj.get("student_id"));
               String numDays = ((String) innerInnerJsonObj.get("num_days"));
               String startDate = ((String) innerInnerJsonObj.get("start_date"));
               String suspensionTypeID = ((String) innerInnerJsonObj.get("suspension_type_id"));
               String active = ((String) innerInnerJsonObj.get("active"));
               
               //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
               stmt = c.prepareStatement("INSERT INTO " + tableName +
                  " (INCIDENT_SUSPENSION_ID, INCIDENT_ID, INCIDENT_STUDENT_ID, SR_STUDENT_ID, NUM_DAYS, START_DATE, SUSPENSION_TYPE_ID, ACTIVE) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                  
               stmt.setString(1, incidentSuspenionID);
               stmt.setString(2, incidentID);
               stmt.setString(3, incidentStudentID);
               stmt.setString(4, SRStudentID);
               stmt.setString(5, numDays);
               stmt.setString(6, startDate);
               stmt.setString(7, suspensionTypeID);
               stmt.setString(8, active);
               
               //execute the insert statement
               stmt.executeUpdate();
               
               recordsProcessed++;
            } //end inner for-loop that gets the suspension records within each separate incident
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