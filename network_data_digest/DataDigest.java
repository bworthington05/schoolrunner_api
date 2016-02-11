//class the puts together all parts of the data digest and prepares them to be sent in an email
public class DataDigest {
  
  public static void main(String args[]) throws Exception {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB.db";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    
    String attMinDate = "2016-02-01";
    String attMaxDate = "2016-02-07";
    
    String assmtMinDate = "2016-01-06";
    String assmtMaxDate = "2016-02-07";
    
    String termBinStartDate = "2015-12-19";
    
    //do the attendance part of the data digest
    AttendanceComparison attendance = new AttendanceComparison(database, attMinDate, attMaxDate);
    
    //file path for attendance JPEG, referenced in email
    String attendanceChart = attendance.run();
    String attendanceMessage = attendance.getEmailMessage();
    String attendanceLinks = attendance.getAnalysisLinks();
    
    AssessmentComparisonRaw assmtRaw = new AssessmentComparisonRaw(database, assmtMinDate, assmtMaxDate);
    String assmtRawChart = assmtRaw.run();
    String assmtRawMessage = assmtRaw.getEmailMessage();

    AssessmentComparisonRelative assmtRelative = new AssessmentComparisonRelative(database, assmtMinDate, assmtMaxDate);
    String assmtRelativeChart = assmtRelative.run();
    String assmtRelativeMessage = assmtRelative.getEmailMessage();
    
    UnalignedAssessmentComparison unaligned = new UnalignedAssessmentComparison(database, assmtMinDate, assmtMaxDate);
    String unalignedChart = unaligned.run();
    String unalignedMessage = unaligned.getEmailMessage();
    String unalignedCSV = unaligned.getUnalignedAssessmentsCSV();
    
    CourseGradesComparison grades = new CourseGradesComparison(database, termBinStartDate);
    grades.run();
    String nfSciSSChart = grades.getChartFile(0);
    String elaChart = grades.getChartFile(1);
    String mathChart = grades.getChartFile(2);
    String gradesMessage = grades.getEmailMessage();
    String gradesLinks = grades.getAnalysisLinks();
    
    EmailDataDigest email = new EmailDataDigest(
      attendanceMessage, attendanceChart, attendanceLinks,
      assmtRawMessage, assmtRawChart,
      assmtRelativeMessage, assmtRelativeChart,
      unalignedMessage, unalignedChart, unalignedCSV,
      gradesMessage, elaChart, mathChart, nfSciSSChart, gradesLinks);
    
    email.send();
    
  } //end main method
  
} //end class

