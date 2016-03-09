import java.util.ArrayList;

public class Test {
    
    public static void main(String[] args) {
        
      //arrays list that will hold assessment info and then be passed to email method
      ArrayList<String> courseName = new ArrayList<String>();
      ArrayList<String> staffEmail = new ArrayList<String>();
      ArrayList<String> assessmentName = new ArrayList<String>();
      ArrayList<String> assessmentDate = new ArrayList<String>();
      ArrayList<String> studentsAssessed = new ArrayList<String>();
      ArrayList<String> editURL = new ArrayList<String>();
      
      for (int i = 0; i < 150; i++) {
          
          courseName.add("test " + i);
          staffEmail.add("bworthington05@gmail.com");
          assessmentName.add("test " + i);
          assessmentDate.add("3/9/16");
          studentsAssessed.add("13");
          editURL.add("www.cnn.com");
      }
        
        
	  //create object of class that handles emailing teachers who created assessments without objectives
	  //pass in the relevant array lists to the constructor
      EmailAssessmentsWithoutObjectives email = new EmailAssessmentsWithoutObjectives(
	      courseName, staffEmail, assessmentName, assessmentDate, studentsAssessed, editURL);
      
      //run method executes the email loop- will ask for email login credentials
      email.run();
      
    }
    
}