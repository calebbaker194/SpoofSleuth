package mailparse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import data.FetchNStore;
import launch.Main;
import regex.CommonRegex;

public class MailCentral {
	private static HashMap<String, String> ipData = new HashMap<String, String>();
	@SuppressWarnings("unused")
	private static Main parent;
	private static String accessKey = readAccessKey();
	
	
	public static String getCountryCode(String ipAddress) {
		System.out.println("Check Country For "+ipAddress);
        URL url;
        if(ipData.get(ipAddress) != null)
        {
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

	private static String readAccessKey()
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
