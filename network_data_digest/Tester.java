import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;

public class Tester {
  
  public static void main(String args[]) {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB1.db";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    
    //String SQLiteOrderBy = "schools.ps_school_id, schools.display_name";
    
    String SQLiteOrderBy = 
    
    "CASE schools.display_name " +
         "WHEN 'RCAA - Pre-K to 2nd' THEN 1 " +
         "WHEN 'RCAA - 3rd to 4th' THEN 2 " +
         "WHEN 'RCAA - 5th to 8th' THEN 3 " +
         "WHEN 'STA - Pre-K to 2nd' THEN 4 " +
         "WHEN 'STA - 3rd to 8th' THEN 5 " +
         "WHEN 'DTA - Pre-K to 2nd' THEN 6 " +
         "WHEN 'DTA - 3rd to 5th' THEN 7 " +
         "WHEN 'DTA - 6th to 8th' THEN 8 " +
         "WHEN 'SCH - Pre-K to 3rd' THEN 9 " +
         "WHEN 'SCH - 4th to 5th' THEN 10 " +
         "WHEN 'SCH - 6th to 8th' THEN 11 " +
         "WHEN 'MCPA - Pre-K to 4th' THEN 12 " +
         "WHEN 'MCPA - 5th to 8th' THEN 13 " +
         "END";
         
    //AssessmentComparisonRelative assmt = new AssessmentComparisonRelative(database, "2016-02-22", "2016-02-26", "2015-12-19", SQLiteOrderBy);
    //String a = assmt.run();
    
    //SuspensionComparison suspension = new SuspensionComparison(database, "2015-07-22", "2015-09-26", "2015-12-19", "2016-03-19", SQLiteOrderBy);
    //suspension.run();
    
    
    AttendanceComparison att = new AttendanceComparison(database, "2016-02-22", "2016-02-26", SQLiteOrderBy);
    att.run();
    
    //CourseGradesComparison courseGrades = new CourseGradesComparison(database, "2015-12-19");
    
    //file path for attendance JPEG, referenced in email
    //courseGrades.run();
    
    //System.out.println(courseGrades.getChartFile(0));
    //System.out.println(courseGrades.getChartFile(1));
    //System.out.println(courseGrades.getChartFile(2));
    
  } //end main method
  
} //end class

