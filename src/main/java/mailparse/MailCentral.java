package mailparse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import data.FetchNStore;
import launch.Main;
import mailobject.MailAttachment;
import regex.CommonRegex;

public class MailCentral {
	private static HashMap<String, String> ipData = new HashMap<String, String>();
	@SuppressWarnings("unused")
	private static Main parent;
	private static String accessKey = FetchNStore.readAccessKey();
	
	
	public static String getCountryCode(String ipAddress) {
		System.out.println("Check Country For "+ipAddress);
        URL url;
        if(ipData.get(ipAddress) != null)
        {
        	System.out.println("Country Resolves to "+ipData.get(ipAddress));
        	return ipData.get(ipAddress);
        }
        if(ipAddress.matches(CommonRegex.LOCAL_IP))
        {
        	return "LOCAL";
        }
		try
		{
			url = new URL("http://api.ipstack.com/"+ipAddress+"?access_key="+accessKey);
	        HttpURLConnection con = (HttpURLConnection) url.openConnection();
	        con.setRequestMethod("GET");
	        con.getResponseCode();
	        ObjectMapper mapper = new ObjectMapper();
	        JsonParser jsonParser = mapper.getFactory().createParser(con.getInputStream());
	        @SuppressWarnings("unchecked")
			Map<String, Object> jsonMap = mapper.readValue(jsonParser, Map.class);
	        ipData.put(ipAddress, (String) jsonMap.get("country_code"));
	        
	        Main.saved = false;
	        return (String) jsonMap.get("country_code");
		}
        catch (MalformedURLException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return "UNK";
	}

	public static ArrayList<MailAttachment> getAttachments(Message message) throws Exception {
	    Object content = message.getContent();
	    if (content instanceof String)
	        return null;        

	    if (content instanceof Multipart) {
	        Multipart multipart = (Multipart) content;
	        ArrayList<MailAttachment> result = new ArrayList<MailAttachment>();

	        for (int i = 0; i < multipart.getCount(); i++) {
	            result.addAll(getAttachments(multipart.getBodyPart(i)));
	        }
	        return result;

	    }
	    return null;
	}

	private static ArrayList<MailAttachment> getAttachments(BodyPart part) throws Exception {
		ArrayList<MailAttachment> result = new ArrayList<MailAttachment>();
	    Object content = part.getContent();
	    if (content instanceof InputStream || content instanceof String) {
	        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || StringUtils.isNotBlank(part.getFileName())) {
	            result.add(new MailAttachment(part.getFileName(),part.getInputStream()));
	            return result;
	        } else {
	            return new ArrayList<MailAttachment>();
	        }
	    }

	    if (content instanceof Multipart) {
	            Multipart multipart = (Multipart) content;
	            for (int i = 0; i < multipart.getCount(); i++) {
	                BodyPart bodyPart = multipart.getBodyPart(i);
	                result.addAll(getAttachments(bodyPart));
	            }
	    }
	    return result;
	}
	
	public static void setParent(Main main)
	{
		parent = main;
		ipData = FetchNStore.loadIpData();
	}

	public static void saveIpData()
	{
		FetchNStore.saveIpData(ipData);
	}
}
