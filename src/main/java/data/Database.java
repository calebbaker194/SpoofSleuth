package data;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import mailobject.Domain;
import mailobject.MailApproval;

public class Database {


	public static boolean exists()
	{
		return new File("data/domaindata.sqlite").exists();
	}
	
	private static Connection connect()
	{
		String url = "jdbc:sqlite:data/domaindata.sqlite";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
	}
	
	public static boolean createDatabase()
	{
		Connection conn = null;
		
		try 
		{
			conn = connect();
			if(conn != null)
			{
				conn.setAutoCommit(false);
				DatabaseMetaData meta = conn.getMetaData();
				System.out.println("The driver name is "+ meta.getDriverName());
				
				
				String conf="CREATE TABLE IF NOT EXISTS conf (\n"
						+ " conf_id integer PRIMARY KEY,\n"
						+ " conf_domain_id integer,\n"
						+ " conf_country_code text,\n"
						+ " conf_occurences real\n"
						+ ");";
				
				String accp="CREATE TABLE IF NOT EXISTS accp (\n"
						+ " accp_id integer PRIMARY KEY,\n"
						+ " accp_domain_id integer,\n"
						+ " accp_country_code text,\n"
						+ " accp_occurences real,\n"
						+ " accp_data integer\n"
						+ ");";
				
				String domain="CREATE TABLE IF NOT EXISTS domain (\n"
						+ " domain_id integer PRIMARY KEY,\n"
						+ " domain_name text\n"
						+ ");";
				
				String appSend="CREATE TABLE IF NOT EXISTS appsend (\n"
						+ " appsend_id integer PRIMARY KEY,\n"
						+ " appsend_domain_id integer,\n"
						+ " appsend_app_domain_name text \n"
						+ ");";
				
				String atmpSend="CREATE TABLE IF NOT EXISTS atmpsend (\n"
						+ " atmpsend_id integer PRIMARY KEY,\n"
						+ " atmpsend_domain_id integer,\n"
						+ " atmpsend_app_domain_name text\n"
						+ ");";
				
				// This table is designed to look at things like constant contact and "bounce" the type will refer to wether it is a subdomain of if its a SRS
				// Example would be bounce.e.zoro.com coming from zoro.com VS zoro.constant-contact.com
				String autoRead="CREATE TABLE IF NOT EXISTS autoread (\n"
						+ " autoread_id integer PRIMARY KEY,\n"
						+ " autoread_domain_name text,\n"
						+ " autoread_type text\n"
						+ ");";
				
				
				Statement confQry = conn.createStatement();
				confQry.execute(conf);
				
				Statement accpQry = conn.createStatement();
				accpQry.execute(accp);
				
				Statement domainQry = conn.createStatement();
				domainQry.execute(domain);
				
				Statement appSendQry = conn.createStatement();
				appSendQry.execute(appSend);
				
				Statement atmpSendQry = conn.createStatement();
				atmpSendQry.execute(atmpSend);
				
				Statement autoReadQry = conn.createStatement();
				autoReadQry.execute(autoRead);
				
				conn.commit();
				conn.close();
				return true;
			}
		} catch (SQLException e)
		{
			try
			{
				conn.rollback();
				conn.close();
			} catch (SQLException e1)
			{
				e1.printStackTrace();
			}
			
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Checks approved domain as well as managing approved domain countries
	 * @param country
	 * @param domain
	 * @return -true: if the domain is approved
	 * 		   -false: if the domain is not in the approved list
	 */
	public synchronized static boolean checkApprovedDomain(String country, String domain)
	{
		String qry = "SELECT accp_id, accp_occurences FROM \n"
				+ " accp JOIN domain ON (domain_id = accp_domain_id)"
				+ " WHERE accp_country_code = '"+country+"' AND domain_name = '"+domain+"'";
		
		String domainInfo = "SELECT domain_id FROM domain WHERE domain_name='"+domain+"'";
		System.out.println(domainInfo);
        try (Connection conn = connect();
        		
                Statement stmt  = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                Statement hasDomain = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                
        		// Get the id of the approval entry for this domain and country
        		ResultSet rs    = stmt.executeQuery(qry))
        {
        	// Check to see if the domain has been regestered
        	ResultSet results = hasDomain.executeQuery(domainInfo);
        	boolean hasDomainB = results.next();
        	System.out.println(hasDomainB);
        	hasDomain.close();
        	if(!hasDomainB)
        	{
        		// If the domain is not regestered go ahead and regester it
        		Statement addDomain = conn.createStatement();
        		addDomain.execute("INSERT INTO domain(domain_name) VALUES('"+domain+"')");
        		addDomain.close();
        	}
        	else if(rs.next())
        	{
        		// If the id for the approval exists. add one to the occurences and return true 
        		Statement addAccp = conn.createStatement();
        		addAccp.execute("UPDATE accp SET accp_occurences="+(rs.getInt(2)+1)+" \n"
        				+ " WHERE accp_id="+rs.getInt(1));
        		addAccp.close();
        		return true;
        	}
        	stmt.close();
        	
        	// Check if domain has any approved countries if not the approve the most frequent country that has more then 5 conf logged
        	if(hasDomainB && !conn.createStatement().execute("SELECT accp_id FROM accp JOIN domain ON(domain_id = accp_domain_id) WHERE domain_name ='"+domain+"'"))
        	{
        		ResultSet highestOver5= conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
        									.executeQuery("SELECT conf_id, conf_country_code FROM conf WHERE conf_occurences >= 5 ORDER BY conf_occurences DESC LIMIT 1");
        		
        		// If there are no confirmed countries and there is a country that has been logged 5 times or more
        		if(highestOver5.next())
        		{
        			// if its the country that we are checking then add 1 to the occurences and
        			// move it to the accepted table. also remove it from the conf table and return true
        			if(highestOver5.getString(2).equals(country))
        			{
        				// Create ACCP
        				conn.createStatement().execute("INSERT INTO accp(accp_domain_id, accp_occurences, accp_country_code) SELECT conf_domain_id, conf_occurences + 1, conf_country_code FROM conf WHERE conf_id="+highestOver5.getInt(1));
        				
        			}
        			else //If its not the country just move the record and delete the entry from the conf table;
        			{
        				conn.createStatement().execute("INSERT INTO accp(accp_domain_id, accp_occurences, accp_country_code) SELECT conf_domain_id, conf_occurences, conf_country_code FROM conf WHERE conf_id="+highestOver5.getInt(1));
        			}
        		}
        		// Remove conf
        		conn.createStatement().execute("DELETE FROM conf WHERE conf_id="+highestOver5.getInt(2));
				// Return true only because we have no base for this yet
				return true;
        	}
        	
        	// Check if the domain and country is already in the conf table
        	Statement checkConf = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        	ResultSet cconfRs = checkConf.executeQuery("SELECT conf_id, conf_occurences FROM conf JOIN domain ON (domain_id = conf_domain_id) WHERE domain_name='"+domain+"' AND conf_country_code = '"+country+"'");
        	if(cconfRs.next())
        	{
        		// If it is then add one to the occurences;
        		int conf_id = cconfRs.getInt(1);
        		int occurences = cconfRs.getInt(2);
        		checkConf.close();
        		
        		Statement addConfOcc = conn.createStatement();
        		addConfOcc.execute("UPDATE conf SET conf_occurences="+(occurences+1)+"\n"
        				+ " WHERE conf_id="+conf_id);
        	}
        	else
        	{
        		// if not the insert an entry into the conf table to allow for proper confirmation
            	Statement addConf = conn.createStatement();
            	addConf.execute("INSERT INTO conf(conf_domain_id,conf_country_code,conf_occurences) SELECT domain_id, '"+country+"', 1 FROM domain WHERE domain_name ='"+domain+"'");
        	}
        	
        	
        	conn.close();
        } 
        catch (SQLException e) 
        {
        	e.printStackTrace();
        }
        if(country.equals("US"))
        	return true;
        
		return false;
	}
	
	public static ArrayList<MailApproval> getDomainForApproval() 
	{
		ArrayList<MailApproval> tmp = new ArrayList<MailApproval>();
		
		try (Connection conn = connect();
	          Statement stmt  = conn.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);)
	    {
			ResultSet data = stmt.executeQuery("SELECT DISTINCT conf_domain_id, domain_name FROM conf JOIN domain ON (domain_id = conf_domain_id)");
			
			while(data.next())
			{
				tmp.add(new MailApproval(data.getString(2), data.getInt(1)));
			}
			
			stmt.close();
	
			for(MailApproval domain : tmp)
			{
				data = conn.createStatement().executeQuery("SELECT conf_country_code, conf_occurences FROM conf WHERE conf_domain_id="+domain.getDomainId());
				while(data.next())
				{
					domain.getCountryOccurenecs().put(data.getString(1), data.getInt(2));
				}
			}
			return tmp;
			
	    } catch (SQLException e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public static ArrayList<MailApproval> getDomainApproved()
	{
		ArrayList<MailApproval> tmp = new ArrayList<MailApproval>();
		
		try (Connection conn = connect();
	          Statement stmt  = conn.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);)
	    {
			ResultSet data = stmt.executeQuery("SELECT DISTINCT accp_domain_id, domain_name FROM accp JOIN domain ON (domain_id = accp_domain_id)");
			
			while(data.next())
			{
				tmp.add(new MailApproval(data.getString(2), data.getInt(1)));
			}
			
			stmt.close();
	
			for(MailApproval domain : tmp)
			{
				data = conn.createStatement().executeQuery("SELECT accp_country_code FROM accp WHERE accp_domain_id="+domain.getDomainId());
				while(data.next())
				{
					domain.getCountryOccurenecs().put(data.getString(1), 0);
				}
			}
			return tmp;
			
	    } catch (SQLException e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public static ArrayList<Domain> getDomains()
	{
		ArrayList<Domain> tmp = new ArrayList<Domain>();
		try (Connection conn = connect();
		        Statement stmt  = conn.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				ResultSet data = stmt.executeQuery("SELECT domain_id, domain_name, accp_country_code FROM domain LEFT JOIN accp ON (domain_id = accp_domain_id) ORDER BY accp_occurences DESC LIMIT 1");
			)
		{
			while(data.next())
				tmp.add(new Domain(data.getInt(1), data.getString(2), data.getString(3)));
			
			return tmp;
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
				
		
		return null;
	}
}
