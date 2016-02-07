import java.sql.*;

public class CreateTable {
  
  //variables for name of database and SQL statement to create a table
  private String dbName;
  private String tableName;
  private String createTableStatement;
  
  //constructor that requires String name of database, name of table, and table statement
  public CreateTable(String dbName, String tableName, String createTableStatement) {
    this.dbName = dbName;
    this.tableName = tableName;
    this.createTableStatement = createTableStatement;
  }
  
  public void run() {
    
    Connection c = null;
    Statement stmt = null;
    
    try {
      
      Class.forName("org.sqlite.JDBC");
      c = DriverManager.getConnection("jdbc:sqlite:" + this.dbName);
      System.out.println("opened database successfully");
      
      //if the table already exists, drop it from the database so that it can be recreated as needed without causing an error
      stmt = c.createStatement();
      String fullTableStatement = "DROP TABLE IF EXISTS " + this.tableName;
      stmt.executeUpdate(fullTableStatement);

      //create the new table now
      stmt = c.createStatement();
      fullTableStatement = "CREATE TABLE " + this.tableName + " " + this.createTableStatement;
      stmt.executeUpdate(fullTableStatement);
      
      stmt.close();
      c.close();
      
    } catch (Exception e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      System.exit(0);
    }
    
    System.out.println("created " + this.tableName + " table successfully in " + this.dbName);
    
  } //end run method

} //end class CreateTable
