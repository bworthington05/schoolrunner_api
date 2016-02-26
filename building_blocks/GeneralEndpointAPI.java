import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import java.net.SocketTimeoutException;
import java.sql.*;

//abstract superclass for other API endpoint processing classes
public abstract class GeneralEndpointAPI {
   
   protected String endpoint;
   
   protected String username;
   protected String password;
   
   //variable for name of database 
   protected String dbName;
   protected String tableName;
   
   //variable for counting how many records from the API have been processed
   protected int recordsProcessed;
   
   //method that will process the JSON results returned from API and save them into database
   public abstract void run();
   
   
   //method to loop through multiple page results from the API endpoint
   //returns a JSON array that contains the results of all pages
   protected JSONArray getAllPages(String innerResults) {
      
      //endpoint URL with page number parameter added to the end
      String newEndpoint = "";
      
      JSONArray allPages = new JSONArray();
      
      //get the total number of pages returned from the API
      Long totalPages = getTotalNumberOfPages();
      
      //connect to the API for the total number of pages, each time get a new page's worth of data
      for (int page = 1; page <= totalPages; page++) {
      
   	   try {
   	      
   	      newEndpoint = endpoint + "&page=" + page;
   	      
   	      //create a new connection to SR API
            ConnectToSRAPI connection = new ConnectToSRAPI(newEndpoint, username, password);
         
            String rawData = connection.run();
            
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(rawData);
            
            //drill down from raw data to the "results" stuff
            JSONObject results = (JSONObject) jsonObject.get("results");
            
            //drill down from "results" to the actual stuff we want, append it to the allPages JSON array
            JSONArray jsonArray = (JSONArray) results.get(innerResults);
            
            allPages.addAll(jsonArray);
            
            //print confirmation message
      	   System.out.println("page " + page + " from " + this.tableName + " received\n");
         
   	   } catch (ParseException ex) {
               ex.printStackTrace();
               
         } catch (SocketTimeoutException e) {
               System.err.println(e.getClass().getName() + ": " + e.getMessage());
               //decrement current page so the next loop repeats the request for this same page
               page--;
   	         
   	   } catch (Exception e) {
   	         System.err.println(e.getClass().getName() + ": " + e.getMessage());
   	   }
   	   
      } //end for loop

      return allPages;
      
   } //end getAllPages method
   
   
   //method to find out how many pages are contained by the API
   protected Long getTotalNumberOfPages() {
      
      Long totalPages = null;
      
	   try {
	      
	      //create a new connection to SR API
         ConnectToSRAPI connection = new ConnectToSRAPI(endpoint, username, password);
      
         String rawData = connection.run();
         
         JSONParser jsonParser = new JSONParser();
         JSONObject jsonObject = (JSONObject) jsonParser.parse(rawData);
         
         //drill down from raw data to the "meta" stuff
         JSONObject meta = (JSONObject) jsonObject.get("meta");
         
         //get the total number of pages
         totalPages = (Long) meta.get("total_pages");
         
         //print confirmation message
   	   System.out.println(this.tableName + " endpoint contains " + totalPages + " pages\n");
      
	   } catch (ParseException ex) {
            ex.printStackTrace();
            
      } catch (SocketTimeoutException e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            //run this method again if this exception happens
            getTotalNumberOfPages();
	         
	   } catch (Exception e) {
	         System.err.println(e.getClass().getName() + ": " + e.getMessage());
	   }
	   
	   return totalPages;
      
   } //end getTotalNumberofPages method
   
   
} //end class