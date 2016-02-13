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
    String attendanceMessage = attendance.getEmailMessage();
    String attendanceChart = attendance.run(); 
    
    //do the assessment rate part
    AssessmentComparisonRelative assmtRelative = new AssessmentComparisonRelative(database, assmtMinDate, assmtMaxDate, termBinStartDate);
    String assessmentMessage = assmtRelative.getEmailMessage();
    String assessmentChart = assmtRelative.run();
    
    //do the unaligned assessment part
    UnalignedAssessmentComparison unaligned = new UnalignedAssessmentComparison(database, assmtMinDate, assmtMaxDate);
    String unalignedMessage = unaligned.getEmailMessage();
    String unalignedChart = unaligned.run(); 
    String unalignedCSV = unaligned.getUnalignedAssessmentsCSV();
    
    //do the course grades part
    CourseGradesComparison grades = new CourseGradesComparison(database, termBinStartDate);
    grades.run();
    String gradesMessage = grades.getEmailMessage(); 
    String nfSciSSChart = grades.getChartFile(0); 
    String elaChart = grades.getChartFile(1); 
    String mathChart = grades.getChartFile(2);
    
    String subject = "data digest 4.0";
    
    //how the body of the email is organized
    String htmlText = 
    attendanceMessage + "<img src=\"cid:image0\"><br><br><br>"+
    assessmentMessage + "<img src=\"cid:image1\"><br><br><br>" +
    unalignedMessage + "<img src=\"cid:image2\"><br><br><br>" +
    gradesMessage + "<img src=\"cid:image3\"><br><br>" +
    "<img src=\"cid:image4\"><br><br>" + "<img src=\"cid:image5\"><br><br>";
    
    //be sure to list these in the same order as in the htmlText above
    String[] images = {attendanceChart, assessmentChart, unalignedChart, nfSciSSChart, elaChart, mathChart};
    
    //list attachments in the order: variable then description, variable then description
    String[] attachments = {unalignedCSV, "Unaligned_Assessments"};
    
    //file path for .txt file of email recipients
    String recipients = "/home/ubuntu/workspace/my_github/schoolrunner_api/network_data_digest/email_recipients/test.txt";
    
    EmailDataDigest email = new EmailDataDigest(subject, htmlText, images, attachments, recipients);
    
    email.send();
    
  } //end main method
  
} //end class

