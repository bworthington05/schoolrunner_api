import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.Multipart;

public class SendEmail {
	
	private String username;
	private String password;
	
    private Login login = new Login();
    
    //constructor, runs through login procedures to get username and password for sending email
    public SendEmail() {
        
        System.out.println("EMAIL LOGIN");
        login.setUsername();
        login.setPassword();
        
        //these credentials are used everytime an the send method is called
        this.username = login.getUsername();
        this.password = login.getPassword();
    }
	
	//method that sends the email- requires recipient, subject, text/body
	public void send(String recipient, String subject, String text) {

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		  });

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(recipient));
			message.setSubject(subject);
			
			//create the message part 
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			
			//fill the message with text (String passed in as parameter), allows for HTML formatting
			messageBodyPart.setText(text, "UTF-8", "html");
			
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			
			//put parts in message
			message.setContent(multipart);
			
			//send the email!
			Transport.send(message);
			
			//display confirmation that the email was sent successfully
			System.out.println("email sent to: " + recipient);

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		
	} //end send method
	
} //end class