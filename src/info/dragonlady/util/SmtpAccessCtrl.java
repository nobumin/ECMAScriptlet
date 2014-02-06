package info.dragonlady.util;

import java.net.URL;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SmtpAccessCtrl {
	
	public static final String MAIL_SERVER_KEY           = "smtp-server";
	public static final String MAIL_SUBJECT_KEY          = "subject";
	public static final String MAIL_BODY_KEY             = "body";

	protected Properties properties = new Properties();
	protected String smtpServer = "localhost";
		
	public SmtpAccessCtrl(String msgKey) throws SmtpAccessException {
		try {
			URL propertiesResource = this.getClass().getClassLoader().getResource("info/dragonlady/scriptlet/mail/resources/mail_config.xml");
			properties.loadFromXML(propertiesResource.openStream());
			smtpServer = properties.getProperty(MAIL_SERVER_KEY);
		}
		catch (Exception e) {
			throw new SmtpAccessException(e);
		}
	}
	
	public void sendMail(String to, String from, String fromName, String fromCharcode, String subject, String subjectCharcode, String message, String messageCharcode, String contentType, String contentTransferEncoding) throws SmtpAccessException{
		try {
			Properties prop = System.getProperties();
			prop.setProperty("mail.smtp.host", smtpServer);
			MimeMessage mime = new MimeMessage(Session.getDefaultInstance(prop));

			mime.setFrom(new InternetAddress(from, fromName, fromCharcode));
			mime.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			mime.setSubject(subject, subjectCharcode);
			mime.setText(message.toString(), messageCharcode);
			if(contentType != null) {
				mime.setHeader("Content-Type", contentType);
			}else{
				mime.setHeader("Content-Type", "text/plain");
			}
			if(contentTransferEncoding != null) {
				mime.setHeader("Content-Transfer-Encoding", contentTransferEncoding);
			}else{
				mime.setHeader("Content-Transfer-Encoding", "7bit");
			}
			mime.setSentDate(new java.util.Date());
			Transport.send(mime);
		}
		catch (Exception e) {
			throw new SmtpAccessException(e);
		}
	}
}
