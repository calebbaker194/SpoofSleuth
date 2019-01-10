package mailalert;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Notifier {
	
	private static HashMap<String, String> notifyInfo = readNotifyInfo();
	
	public static void notify(String string, Message suspicious)
	{
		Properties props = System.getProperties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.user", notifyInfo.get("user"));
        props.put("mail.smtp.password", notifyInfo.get("password"));
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try 
        {
            message.setFrom(new InternetAddress("caleb.baker194@gmail.com"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("it@pittsburgsteel.com"));

            message.setSubject("COUNTRY CODE WARNING");           
            message.setText("Email-To: "+suspicious.getRecipients(RecipientType.TO).toString() + "\n" +
            				"    From: "+suspicious.getFrom()[0].toString() + "\n" +
            				" Subject: "+suspicious.getSubject());
            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", notifyInfo.get("user"), notifyInfo.get("password"));
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        }
        catch (AddressException ae) {
            ae.printStackTrace();
        }
        catch (MessagingException me) {
            me.printStackTrace();
        }
    }

	private static HashMap<String, String> readNotifyInfo()
	{
		HashMap<String, String> tmp = new HashMap<String, String>();
		Scanner notinfo;
		try
		{
			notinfo = new Scanner(new File("notifyinfo.data"));
			tmp.put("user", notinfo.nextLine());
			tmp.put("password", notinfo.nextLine());
			notinfo.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return tmp;
	}
}
