import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;

public class Tester {
  
  public static void main(String args[]) {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB.db";
    
    AttendanceComparison attendance = new AttendanceComparison("2016-02-01", "2016-02-07", dbName);
    
    //file path for attendance JPEG, referenced in email
    String attendanceChart = attendance.run();
    
  } //end main method
  
} //end class

