package data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import launch.Main;
import mailobject.ImapServer;

public class FetchNStore {
	public static void saveIpData(HashMap<String, String> ipData)
	{
		try
		{
			if(!new File("data/IpData.map").exists())
			{
				new File("data/IpData.map").createNewFile();
			}
			ObjectOutputStream objectWriter = new ObjectOutputStream(new FileOutputStream("data/IpData.map"));
			objectWriter.writeObject(ipData);
			objectWriter.flush();
			objectWriter.close();
			Main.saved = true;
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		try
		{
			Thread.sleep(30000);
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, String> loadIpData()
	{
		try
		{
			ObjectInputStream objectReader = new ObjectInputStream(new FileInputStream("data/IpData.map"));
			HashMap<String,String> ipData = (HashMap<String, String>) objectReader.readObject();
			objectReader.close();
			Main.saved = true;
			return ipData;
		} catch (IOException e)
		{
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, ImapServer> loadEmailData()
	{
		HashMap<String,ImapServer> emails = new HashMap<String, ImapServer>();
		try
		{
			byte[] emailData = Files.readAllBytes(Paths.get("data/maildata.json"));
			ObjectMapper objectMapper = new ObjectMapper();
			HashMap<String, HashMap<String,Object>> tmp = new HashMap<String , HashMap<String,Object>>();
			tmp = objectMapper.readValue(emailData, new HashMap<String, ImapServer>().getClass());
			for(String email : tmp.keySet())
			{
				emails.put(email, new ImapServer((Boolean) tmp.get(email).get("starttls"),
												 (String)  tmp.get(email).get("imapHost"),
												 (Integer) tmp.get(email).get("imapPort"),
												 (String)  tmp.get(email).get("smtpHost"),
												 (Integer) tmp.get(email).get("smtpPort"),
												 (String)  tmp.get(email).get("imapProtocol"),
												 (String)  tmp.get(email).get("smtpProtocol"),
												 (String)  tmp.get(email).get("username"),
												 (String)  tmp.get(email).get("address"),
												 (String)  tmp.get(email).get("commonName"),
												 (String)  tmp.get(email).get("password"),
												 (Boolean) tmp.get(email).get("auth")));
			}
			
			return emails;
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static HashMap<String, String> readNotifyInfo()
	{
		HashMap<String, String> tmp = new HashMap<String, String>();
		Scanner notinfo;
		try
		{
			notinfo = new Scanner(new File("data/notifyinfo.data"));
			tmp.put("user", notinfo.nextLine());
			tmp.put("password", notinfo.nextLine());
			notinfo.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return tmp;
	}
	
	public static String readAccessKey()
	{
		try
		{
			Scanner apikey = new Scanner(new File("data/ipstack.key"));
			String tmp = apikey.nextLine();
			apikey.close();
			return tmp;
			
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return "";
	}
	
	public void saveEmailData(HashMap<String,ImapServer> emails) {
		ObjectMapper o = new ObjectMapper();
		o.enable(SerializationFeature.INDENT_OUTPUT);
		try
		{
			o.writeValue(new File("data/maildata.json"), emails);
		} catch (JsonGenerationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
