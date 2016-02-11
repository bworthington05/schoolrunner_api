import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;

public class Tester {
  
  public static void main(String args[]) {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB.db";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    
    AttendanceComparison att = new AttendanceComparison(database, "2016-02-01", "2016-02-10");
    
    String a = att.run();
    
    //CourseGradesComparison courseGrades = new CourseGradesComparison(database, "2015-12-19");
    
    //file path for attendance JPEG, referenced in email
    //courseGrades.run();
    
    //System.out.println(courseGrades.getChartFile(0));
    //System.out.println(courseGrades.getChartFile(1));
    //System.out.println(courseGrades.getChartFile(2));
    
  } //end main method
  
} //end class

