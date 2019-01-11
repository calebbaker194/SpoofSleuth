package mailalert;

import java.util.HashMap;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import data.FetchNStore;

public class Notifier {
	
	private static HashMap<String, String> notifyInfo = FetchNStore.readNotifyInfo();
	
	public static void notify(int check, HashMap<String, String> data)
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
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("caleb.baker194@gmail.com"));
            
            String subject = "";
            
            subject += check > 8 ? "MACRO DETECTED: " : "" ; // check for 8
            subject += check % 8 > 3 ? "POSSIBLE SPOOF: " : "" ; // check for 4
            subject += check % 4 > 1 ? "SPF FAILURE: " : "" ; // check for 2
            subject += check % 2 == 1 ? "COUNRTY CODE WARNING: " : "" ; //check for 1
            
            message.setSubject(subject); 
            
            String text = "";
            
            text += check > 3 ? "Return-Path: "+data.get("returnPath")+"\nFrom: "+data.get("from")+"\n" : "" ;
            text += check % 4 > 1 ? "SPF-Result: "+data.get("spfResult") + "\n" : "" ;
            text += "Domain: " + data.get("domain") + "\n";
            text += check % 2 == 1 ? "Country Code: " + data.get("countryCode") +"\n\n" : "\n" ;
            text += "To: " + data.get("to") +"\n";
            text += "Subject: " + data.get("subject")+"\n\n";
            text += "Body: \n" + data.get("body");
            
            
            if(subject.contains("MACRO"))
            {
            	text += "Corrupt Files:\n";
            	int files = Integer.parseInt(data.get("macroCount"));
            	for(int x=0; x < files; x++)
            	{
            		text+= "Files: "+data.get("infected"+x);
            	}
            }
            
            message.setText(text);
            
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
}
