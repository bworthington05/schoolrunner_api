//class the puts together all parts of the data digest and prepares them to be sent in an email
public class DataDigest {
  
  public static void main(String args[]) throws Exception {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB.db";
    
    DatabaseSetup database = new DatabaseSetup(dbName);
    
    String subject = "Network Data Digest 3/4/2016"; //change this
    
    String attMinDate = "2016-02-29"; //change this
    String attMaxDate = "2016-03-04"; //change this
    
    String assmtMinDate = "2015-12-19"; //change this
    String assmtMaxDate = "2016-03-04"; //change this
    
    String q1StartDate = "2015-07-22";
    String q2StartDate = "2015-09-26"; 
    String q3StartDate = "2015-12-19";
    String q4StartDate = "2016-03-19";
    
    String termBinStartDate = q3StartDate;
    
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
    
    //do the attendance part of the data digest
    AttendanceComparison attendance = new AttendanceComparison(database, attMinDate, attMaxDate, SQLiteOrderBy);
    String attendanceMessage = attendance.getEmailMessage();
    String attendanceChart = attendance.run(); 
    
    //do the assessment rate part
    AssessmentComparisonRelative assmtRelative = new AssessmentComparisonRelative(database, assmtMinDate, 
      assmtMaxDate, termBinStartDate, SQLiteOrderBy);
    String assessmentMessage = assmtRelative.getEmailMessage();
    String assessmentChart = assmtRelative.run();
    
    //do the unaligned assessment part
    UnalignedAssessmentComparison unaligned = new UnalignedAssessmentComparison(database, assmtMinDate,
      assmtMaxDate, SQLiteOrderBy);
    String unalignedMessage = unaligned.getEmailMessage();
    String unalignedChart = unaligned.run(); 
    String unalignedCSV = unaligned.getUnalignedAssessmentsCSV();
    
    //do the course grades part
    CourseGradesComparison grades = new CourseGradesComparison(database, termBinStartDate, SQLiteOrderBy);
    grades.run();
    String gradesMessage = grades.getEmailMessage(); 
    String nfSciSSChart = grades.getChartFile(0); 
    String elaChart = grades.getChartFile(1); 
    String mathChart = grades.getChartFile(2);
    
    //do the suspension rate part
    SuspensionComparison suspension = new SuspensionComparison(database, q1StartDate, q2StartDate, q3StartDate, q4StartDate, SQLiteOrderBy);
    String suspensionMessage = suspension.getEmailMessage();
    String suspensionChart = suspension.run();
    
    //how the body of the email is organized
    String htmlText = 
    attendanceMessage + "<img src=\"cid:image0\"><br><br><br>" +
    suspensionMessage + "<img src=\"cid:image1\"><br><br><br>" +
    assessmentMessage + "<img src=\"cid:image2\"><br><br><br>" +
    unalignedMessage + "<img src=\"cid:image3\"><br><br><br>" +
    gradesMessage + "<img src=\"cid:image4\"><br><br>" +
    "<img src=\"cid:image5\"><br><br>" + "<img src=\"cid:image6\"><br><br><br>";
    
    //be sure to list these in the same order as in the htmlText above
    String[] images = {attendanceChart, suspensionChart, assessmentChart, unalignedChart, elaChart, mathChart, nfSciSSChart};
    
    //list attachments in the order: variable then description, variable then description
    String[] attachments = {unalignedCSV, "Unaligned_Assessments.csv"};
    
    //file path for .txt file of email recipients
    String recipients = "/home/ubuntu/workspace/my_github/schoolrunner_api/network_data_digest/email_recipients/test.txt";
    
    EmailDataDigest email = new EmailDataDigest(subject, htmlText, images, attachments, recipients);
    
    email.send();
    
  } //end main method
  
} //end class

