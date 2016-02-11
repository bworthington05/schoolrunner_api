import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import java.util.Properties;

public class EmailDataDigest {
    
    //all the components of the data digest email
    private String attendanceMessage;
    private String attendanceChart;
    private String attendanceLinks;
    private String assmtRawMessage;
    private String assmtRawChart;
    private String assmtRelativeMessage;
    private String assmtRelativeChart;
    private String unalignedMessage;
    private String unalignedChart;
    private String unalignedCSV;
    private String gradesMessage;
    private String elaChart;
    private String mathChart;
    private String nfSciSSChart;
    private String gradesLinks;
    
	private String username;
	private String password;
    private Login login = new Login();

    //constructor requires String values for each part of the email
    public EmailDataDigest(
        String attendanceMessage, String attendanceChart, String attendanceLinks,
        String assmtRawMessage, String assmtRawChart,
        String assmtRelativeMessage, String assmtRelativeChart,
        String unalignedMessage, String unalignedChart, String unalignedCSV,
        String gradesMessage, String elaChart, String mathChart, String nfSciSSChart, String gradesLinks) {
        
        System.out.println("EMAIL LOGIN");
        this.login.setUsername();
        this.login.setPassword();
        
        //these credentials are used everytime the send method is called
        this.username = login.getUsername();
        this.password = login.getPassword();
        
        this.attendanceMessage = attendanceMessage;
        this.attendanceChart = attendanceChart;
        this.attendanceLinks = attendanceLinks;
        
        this.assmtRawMessage = assmtRawMessage;
        this.assmtRawChart = assmtRawChart;
        
        this.assmtRelativeMessage = assmtRelativeMessage;
        this.assmtRelativeChart = assmtRelativeChart;
        
        this.unalignedMessage = unalignedMessage;
        this.unalignedChart = unalignedChart;
        this.unalignedCSV = unalignedCSV;
        
        this.gradesMessage = gradesMessage;
        this.elaChart = elaChart;
        this.mathChart = mathChart;
        this.nfSciSSChart = nfSciSSChart;
        this.gradesLinks = gradesLinks;
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
        message.setSubject("Data Digest Test 4");
        message.setFrom(new InternetAddress(username));
        message.addRecipient(Message.RecipientType.TO,
            new InternetAddress("bworthington05@gmail.com"));

        message.addRecipient(Message.RecipientType.TO,
            new InternetAddress("bworthington@renewschools.org"));


        MimeMultipart multipart = new MimeMultipart("related");

        BodyPart messageBodyPart = new MimeBodyPart();
        String htmlText = 
            this.attendanceMessage + "<img src=\"cid:image1\"><br><br>" + this.attendanceLinks + "<br>" +
            this.assmtRawMessage + "<img src=\"cid:image2\"><br><br><br>" +
            this.assmtRelativeMessage + "<img src=\"cid:image3\"><br><br><br>" +
            this.unalignedMessage + "<img src=\"cid:image4\"><br><br><br>" +
            this.gradesMessage + "<img src=\"cid:image5\"><br><br>" +
            "<img src=\"cid:image6\"><br><br>" + "<img src=\"cid:image7\"><br><br>" +
            this.gradesLinks + "<br>";
            
        messageBodyPart.setContent(htmlText, "text/html");

        //add the htmlText content
        multipart.addBodyPart(messageBodyPart);
        
        //now add the images/attachments
        messageBodyPart = new MimeBodyPart();
        DataSource fds = new FileDataSource(this.attendanceChart);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID","<image1>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart();
        fds = new FileDataSource(this.assmtRawChart);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID","<image2>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart();
        fds = new FileDataSource(this.assmtRelativeChart);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID","<image3>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart();
        fds = new FileDataSource(this.unalignedChart);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID","<image4>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart();
        fds = new FileDataSource(this.elaChart);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID","<image5>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart();
        fds = new FileDataSource(this.mathChart);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID","<image6>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart();
        fds = new FileDataSource(this.nfSciSSChart);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setHeader("Content-ID","<image7>");
        multipart.addBodyPart(messageBodyPart);
        
        messageBodyPart = new MimeBodyPart();
        fds = new FileDataSource(this.unalignedCSV);
        messageBodyPart.setDataHandler(new DataHandler(fds));
        messageBodyPart.setFileName("Unaligned_Assessments.csv");
        multipart.addBodyPart(messageBodyPart);

        //put everything together
        message.setContent(multipart);

        transport.connect();
        transport.sendMessage(message, message.getRecipients(Message.RecipientType.TO));
        transport.close();
        
        System.out.println("email sent");
        
    } //end send method
    
} //end class