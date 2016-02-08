
public class DataDigest {
  
  public static void main(String args[]) throws Exception {
    
    //file path for database
    String dbName = "/home/ubuntu/workspace/databases/SRDB.db";
    
    AttendanceComparison attendance = new AttendanceComparison("2016-02-01", "2016-02-07", dbName);
    
    //file path for attendance JPEG, referenced in email
    String attendanceChart = attendance.run();
    String attendanceMessage = attendance.getAttendanceEmailMessage();
    String attendanceLinks = attendance.getAttendanceAnalysisLinks();
    
    EmailDataDigest email = new EmailDataDigest(attendanceMessage, attendanceChart, attendanceLinks);
    email.send();
    
  } //end main method
  
} //end class

