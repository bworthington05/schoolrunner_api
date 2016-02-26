import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.util.Properties;
import java.util.ArrayList;
import java.io.*;
import java.util.Scanner;

public class EmailDataDigest {
    
    //all the components of the email
    private String subject;
    private String htmlText;
    private String[] images;
    private String[] attachments;
    private String recipientsFile;
    private ArrayList<String> recipientsArray = new ArrayList<String>();
    
	private String username;
	private String password;
    private Login login = new Login();
    
    //email stuff that needs to be available to all methofs
    private Transport transport;
    private MimeMessage message;
    private MimeMultipart multipart;
    private BodyPart messageBodyPart;

    //constructor requires String values for each part of the email
    public EmailDataDigest(String subject, String htmlText, String[] images, String[] attachments, String recipientsFile) {
        
        this.subject = subject;
        this.htmlText = htmlText;
        this.images = images;
        this.attachments = attachments;
        this.recipientsFile = recipientsFile;
        
        System.out.println("EMAIL LOGIN");
        this.login.setUsername();
        this.login.setPassword();
        
        //these credentials are used everytime the send method is called
        this.username = login.getUsername();
        this.password = login.getPassword();
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
		  
        this.transport = mailSession.getTransport();

        this.message = new MimeMessage(mailSession);
        this.message.setSubject(this.subject);
        this.message.setFrom(new InternetAddress(username));
        
        //prepare array list with recipients for .txt file
        getRecipients();
        
        //now loop through the array and add each recipient's email
        for(int n = 0; n < this.recipientsArray.size(); n++) {
        
            this.message.addRecipient(Message.RecipientType.TO,
                new InternetAddress(this.recipientsArray.get(n)));
        }

        this.multipart = new MimeMultipart("related");
        this.messageBodyPart = new MimeBodyPart();
        //add the htmlText content    
        this.messageBodyPart.setContent(this.htmlText, "text/html");
        this.multipart.addBodyPart(messageBodyPart);
        
        //loop through the images array and add each image to the email
        for(int n = 0; n < images.length; n++) {
        
            this.messageBodyPart = new MimeBodyPart();
            DataSource fds = new FileDataSource(this.images[n]);
            this.messageBodyPart.setDataHandler(new DataHandler(fds));
            this.messageBodyPart.setHeader("Content-ID","<image" + n + ">");
            this.multipart.addBodyPart(this.messageBodyPart);
        }
        
        //loop through the attachments array and add each attachment to the email
        for(int n = 0; n < this.attachments.length; n += 2) {

            this.messageBodyPart = new MimeBodyPart();
            DataSource fds = new FileDataSource(this.attachments[n]);
            this.messageBodyPart.setDataHandler(new DataHandler(fds));
            this.messageBodyPart.setFileName(this.attachments[n + 1]);
            this.multipart.addBodyPart(this.messageBodyPart);
        }

        //put everything together
        this.message.setContent(this.multipart);

        this.transport.connect();
        this.transport.sendMessage(this.message, this.message.getRecipients(Message.RecipientType.TO));
        this.transport.close();
        
        System.out.println("email sent");
        
    } //end send method
    
    //method to populate an array list with email addresses contained in a .txt file
    private void getRecipients() throws FileNotFoundException {

        //read the file that contains the list of recipients
        Scanner scanner = new Scanner(new File(this.recipientsFile));
    
        while(scanner.hasNextLine()) {
            
            String email = scanner.nextLine();
            recipientsArray.add(email);
        }
        
        scanner.close();
    }
    
} //end class