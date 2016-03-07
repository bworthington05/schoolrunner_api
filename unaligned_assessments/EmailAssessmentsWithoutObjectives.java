import java.util.ArrayList;
import javax.mail.AuthenticationFailedException;

public class EmailAssessmentsWithoutObjectives {
    
    //array lists that will hold the info needed for each email
    private ArrayList<String> courseName = new ArrayList<String>();
    private ArrayList<String> staffEmail = new ArrayList<String>();
    private ArrayList<String> assessmentName = new ArrayList<String>();
    private ArrayList<String> assessmentDate = new ArrayList<String>();
    private ArrayList<String> studentsAssessed = new ArrayList<String>();
    private ArrayList<String> editURL = new ArrayList<String>();
    
    //constructor that requires array lists with the info needed for each email
    public EmailAssessmentsWithoutObjectives(
        ArrayList<String> courseName,
        ArrayList<String> staffEmail,
        ArrayList<String> assessmentName,
        ArrayList<String> assessmentDate,
        ArrayList<String> studentsAssessed,
        ArrayList<String> editURL) {
        
        this.courseName = courseName;
        this.staffEmail = staffEmail;
        this.assessmentName = assessmentName;
        this.assessmentDate = assessmentDate;
        this.studentsAssessed = studentsAssessed;
        this.editURL = editURL;
    }
    
    public void run() {
        
        //create SendEmail object, which will then prompt user for email login credentials
        SendEmail sendEmail = new SendEmail();
        
        //counters to track and output the total number of emails sent and failed
        int sentEmails = 0;
        int failedEmails = 0;
    
        for (int i = 0; i < courseName.size(); i++) {
            
            //every 50 emails, pause for 60 seconds so google doesn't complain about too many login attempts
            //there is probably a better way to handle this issue, just don't know it yet...
                if ((i % 50 == 0) && (i > 0 )) {
                    
                    try {
                        System.out.println("pausing for 60 seconds...\n");
                        Thread.sleep(60000);
        
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }
    
            String recipient = this.staffEmail.get(i);
            String courseName = this.courseName.get(i);
            String assessmentName = this.assessmentName.get(i);
            String assessmentDate = this.assessmentDate.get(i);
            String studentsAssessed = this.studentsAssessed.get(i);
            String editURL = this.editURL.get(i);
            
            String subject = "<ACTION REQUIRED> Missing Objectives in SR Assessment: " + assessmentName;
            String assessmentLink = "<a href='" + editURL + "'>CLICK HERE</a>";
            
            String videoURL = "http://youtu.be/ZqS4r0MbkwM";
            String videoLink = "<a href='" + videoURL + "'>video screencast</a>";
            
            String text = 
                "You are receiving this email because you created the following assessment in SchoolRunner that has " +
                "one or more questions that are not aligned to objectives. Because we use a standards-based " +
                "grading system, currently data in this assessment will <u>not</u> count towards " +
                "students' final grades.<br><br>" +
                
                "Name: <strong>" + assessmentName + "</strong><br>" +
                "Course: <strong>" + courseName + "</strong><br>" +
                "Date: <strong>" + assessmentDate + "</strong><br>" +
                "Number of Results: <strong>" + studentsAssessed + "</strong><br><br>" +
                
                "In order to edit this assessment and align it to objectives <strong><font size='3'>" + assessmentLink + "</font size='3'></strong><br><br>" +
                
                "You can watch this <strong>" + videoLink + "</strong> for a quick demonstration of how to fix an assessment that needs to be aligned to objectives.<br><br>" +
                
                "Please contact us at data@renewschools.org if you have any questions or believe this email " +
                "was a mistake.";
                
            try {
                
                sendEmail.send(recipient, subject, text);
                sentEmails++;
            
            //catch exceptions for too many email login attempts, pause for a while, then try this email again
            } catch (AuthenticationFailedException e) {
                System.out.println("ERROR with index " + i + ", email not sent to " + recipient);
                System.out.println(e);
                System.out.println("Pausing for 4 minutes and then attempting this email again...\n");
                    try {
                        Thread.sleep(240000);
        
                    } catch (InterruptedException ie) {
                        System.out.println(ie);
                    }
                    
                //decrement i so the next email attempt tries this index again
                i--;

            //catch any other exceptions that may occur when email is attempted, output error message
            } catch (RuntimeException e) {
                System.out.println("ERROR with index " + i + ", email not sent to " + recipient);
                System.out.println(e);
                failedEmails++;
            }
            
            System.out.println("index " + i + " processed\n");
            
        }
        
        System.out.println("email loop complete, total emails sent: " + sentEmails);
        System.out.println("total failed emails due to exceptions: " + failedEmails);
        
    } //end run method
    
} //end class