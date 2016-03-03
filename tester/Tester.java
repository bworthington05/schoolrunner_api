import java.sql.*;
import java.io.FileWriter;
import java.io.IOException;

public class Tester {
  
  public static void main(String args[]) {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/Test1.db";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    database.createIncidentSuspensionsTable("https://renew.schoolrunner.org/api/v1/incidents?limit=30000&active=1&min_date=2015-07-22&with_related=true&school_ids=13");
    database.createIncidentsTable("https://renew.schoolrunner.org/api/v1/incidents?limit=30000&active=1&min_date=2015-07-22&school_ids=13");
    //database.createCoursesTable("https://renew.schoolrunner.org/api/v1/courses/?limit=700");
    //database.createSchoolsTable("https://renew.schoolrunner.org/api/v1/schools/?limit=6");
    //database.createStaffTable("https://renew.schoolrunner.org/api/v1/staff-members/?limit=700");
    //database.createStudentsTable("https://renew.schoolrunner.org/api/v1/students/?limit=700");
    //database.createAbsencesTable("https://renew.schoolrunner.org/api/v1/absences/?limit=30000&min_date=2015-07-22&absence_type_ids=24");
    //database.createAssessmentsTable("https://renew.schoolrunner.org/api/v1/assessments/?limit=3000&active=1&min_date=2015-07-22");
    
  } //end main method
  
} //end class

