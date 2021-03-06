import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;

public class Tester {
  
  public static void main(String args[]) {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB1.db";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    
    String SQLiteOrderBy = 
      "schools.display_name, min_out_of_class.total DESC, students.last_name, students.first_name";
    
    MinutesOutOfClass minutes = new MinutesOutOfClass(database, "2015-07-22", SQLiteOrderBy);
    minutes.run();
    
  } //end main method
  
} //end class

