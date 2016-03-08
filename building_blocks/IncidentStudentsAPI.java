import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.sql.*;

//class that gets data from incidents API endpoint, but specifically from the incident_students blocks
//assumes regular incidents table is being created or has already been created too
public class IncidentStudentsAPI extends GeneralEndpointAPI {
   
   //constructor that takes the username and password
   //which will be passed to the ConnectToSRAPI constructor
   //constructor also requires the names of database and tables being used
   public IncidentStudentsAPI(String username, String password, String dbName, String tableName) {
      this.username = username;
      this.password = password;
      this.dbName = dbName;
      this.tableName = tableName;
      
      //defdault API endpoint url, includes extra parameters for active, min_date, and with_related (to display consequence details)
      this.endpoint = "https://renew.schoolrunner.org/api/v1/incidents/?limit=30000&active=1&min_date=2015-07-22&with_related=true";
   }

   //alternate constructor that also accepts a String endpoint
   //useful incase an endpoint url with parameters should be used instead of the default
   public IncidentStudentsAPI(String username, String password, String dbName, String tableName, String endpoint) {
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
         
         //loop through the JSON array and get the inner JSON array of students records within each incident
         for (int i = 0; i < innerJsonArray.size(); i++) {
            JSONObject innerJsonObj = (JSONObject) innerJsonArray.get(i);
               
            JSONArray innerInnerJsonArray = (JSONArray) innerJsonObj.get("incident_students");
            
            for (int n = 0; n < innerInnerJsonArray.size(); n++) {
               
               JSONObject innerInnerJsonObj = (JSONObject) innerInnerJsonArray.get(n);
            
               //save data from innerInnerJsonObj (students records) to temporary String variables that will be used in a SQL insert statement
               String incidentStudentID = ((String) innerInnerJsonObj.get("incident_student_id"));
               String incidentID = ((String) innerInnerJsonObj.get("incident_id"));
               String SRStudentID = ((String) innerInnerJsonObj.get("student_id"));
               String incidentRoleID = ((String) innerInnerJsonObj.get("incident_role_id"));
               String minOutOfClass = ((String) innerInnerJsonObj.get("minutes_out_of_class"));
               String active = ((String) innerInnerJsonObj.get("active"));
               
               //PreparedStatement provides security from SQL injection attack, allows text to contain single quotes (like some names do)
               stmt = c.prepareStatement("INSERT INTO " + tableName +
                  " (INCIDENT_STUDENT_ID, INCIDENT_ID, SR_STUDENT_ID, INCIDENT_ROLE_ID, MINUTES_OUT_OF_CLASS, ACTIVE) " +
                  "VALUES (?, ?, ?, ?, ?, ?)");
                  
               stmt.setString(1, incidentStudentID);
               stmt.setString(2, incidentID);
               stmt.setString(3, SRStudentID);
               stmt.setString(4, incidentRoleID);
               stmt.setString(5, minOutOfClass);
               stmt.setString(6, active);
               
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