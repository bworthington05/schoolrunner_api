import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.util.Properties;

public class EmailDataDigest {
    
	private String username;
	private String password;
    private String attendanceMessage;
    private String attendanceChart;
    private String attendanceLinks;
    private Login login = new Login();

    //constructor requires String values for each part of the email
    public EmailDataDigest(String attendanceMessage, String attendanceChart, String attendanceLinks) {
        
        System.out.println("EMAIL LOGIN");
        this.login.setUsername();
        this.login.setPassword();
        
        //these credentials are used everytime the send method is called
        this.username = login.getUsername();
        this.password = login.getPassword();
        
        this.attendanceMessage = attendanceMessage;
        this.attendanceChart = attendanceChart;
        this.attendanceLinks = attendanceLinks;
    }
    
    public void send() throws Exception {

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session mailSession = Session.getInstance(props,
		    new javax.mail.Authenticator() {
			    protected PasswordAuthentication getPasswordAuthentication() {
				    return new PasswordAuthentication(username, password);
			    }
            });
		  
        Transport transport = mailSession.getTransport();

        MimeMessage message = new MimeMessage(mailSession);
        message.setSubject("Data Digest Test2");
        message.setFrom(new InternetAddress(username));
        message.addRecipient(Message.RecipientType.TO,
            new InternetAddress("bworthington05@gmail.com"));

        message.addRecipient(Message.RecipientType.TO,
            new InternetAddress("sumeet@renewschools.org"));


        MimeMultipart multipart = new MimeMultipart("related");

        //attendance message + attendance chart + attendance report links
        BodyPart messageBodyPart = new MimeBodyPart();
        String htmlText = this.attendanceMessage + "<img src=\"cid:image\"><br><br>" + this.attendanceLinks;
        messageBodyPart.setContent(htmlText, "text/html");

        //add it
        multipart.addBodyPart(messageBodyPart);
        
        //now the image
        messageBodyPart = new MimeBodyPart();
        
        String file = attendanceChart;
        DataSource fds = new FileDataSource(file);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID","<image>");

        //add it
        multipart.addBodyPart(messageBodyPart);

        //put everything together
        message.setContent(multipart);

        transport.connect();
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
        
        System.out.println("email sent");
        
    } //end send method
    
} //end class