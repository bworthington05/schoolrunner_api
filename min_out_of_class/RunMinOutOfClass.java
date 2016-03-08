public class RunMinOutOfClass {
  
  public static void main(String args[]) {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB1.db";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    
    String min_date = "2015-07-22";
    
    String SQLiteOrderBy = 
      "schools.display_name, min_out_of_class.total DESC, students.last_name, students.first_name";
    
    MinutesOutOfClass minutesOutOfClass = new MinutesOutOfClass(database, min_date, SQLiteOrderBy);
    minutesOutOfClass.run();
    
  } //end main method
  
} //end class