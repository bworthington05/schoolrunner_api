import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;

public class Tester {
  
  public static void main(String args[]) {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB1.db";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    
    AssessmentComparisonRelative assmt = new AssessmentComparisonRelative(database, "2016-01-01", "2016-02-10", "2015-12-19");
    
    String a = assmt.run();
    
    //CourseGradesComparison courseGrades = new CourseGradesComparison(database, "2015-12-19");
    
    //file path for attendance JPEG, referenced in email
    //courseGrades.run();
    
    //System.out.println(courseGrades.getChartFile(0));
    //System.out.println(courseGrades.getChartFile(1));
    //System.out.println(courseGrades.getChartFile(2));
    
  } //end main method
  
} //end class

