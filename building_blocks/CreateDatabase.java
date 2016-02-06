import java.sql.*;

public class CreateDatabase {
  
  //variable for name of database 
  private String dbName;
  
  //constructor that requires String name of database to create
  public CreateDatabase(String dbName) {
    this.dbName = dbName;
  }
  
  public void run() {
    Connection c = null;
    
    try {
      
      Class.forName("org.sqlite.JDBC");
      c = DriverManager.getConnection("jdbc:sqlite:" + this.dbName);
      
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
    }
    
    System.out.println("created database successfully");
    
  } //end run method
  
} //end class CreateDatabase