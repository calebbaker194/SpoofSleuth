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
		return new File("domaindata.sqlite").exists();
	}
	
	private static Connection connect()
	{
		String url = "jdbc:sqlite:data/domaindata.sqlite";
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
						+ " domain_name text,\n"
						+ " domain_country\n"
						+ ");";
				
				Statement confQry = conn.createStatement();
				confQry.execute(conf);
				
				Statement accpQry = conn.createStatement();
				accpQry.execute(accp);
				
				Statement domainQry = conn.createStatement();
				domainQry.execute(domain);
				
				
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
	
	public synchronized static boolean checkApprovedDomain(String country, String domain)
	{
		String qry = "SELECT accp_id, accp_occurences FROM \n"
				+ " accp JOIN domain ON (domain_id = accp_domain_id)"
				+ " WHERE accp_country_code = '"+country+"' AND domain_name = '"+domain+"'";
		
        try (Connection conn = connect();
                Statement stmt  = conn.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                ResultSet rs    = stmt.executeQuery(qry))
        {
        	if(rs.next())
        	{
        		Statement addAccp = conn.createStatement();
        		addAccp.execute("UPDATE accp SET accp_occurences="+(rs.getInt(1)+1)+" \n"
        				+ " WHERE accp_id="+rs.getInt(0));
        		
        		return true;
        	}
        	stmt.close();
        	// Check if the domain and country is already in the conf table
        	Statement checkConf = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        	ResultSet cconfRs = checkConf.executeQuery("SELECT conf_id, conf_occurences FROM conf JOIN domain ON (domain_id = conf_domain_id) WHERE domain_name='"+domain+"' AND conf_country_code = '"+country+"'");
        	if(cconfRs.next())
        	{
        		int conf_id = cconfRs.getInt(0);
        		int occurences = cconfRs.getInt(1);
        		checkConf.close();
        		
        		Statement addConfOcc = conn.createStatement();
        		addConfOcc.execute("UPDATE conf SET conf_occurences="+(occurences+1)+"\n"
        				+ " WHERE conf_id="+conf_id);
        	}
        	else
        	{
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
				tmp.add(new MailApproval(data.getString(1), data.getInt(0)));
			}
			
			stmt.close();
	
			for(MailApproval domain : tmp)
			{
				data = conn.createStatement().executeQuery("SELECT conf_country_code, conf_occurences FROM conf WHERE conf_domain_id="+domain.getDomainId());
				while(data.next())
				{
					domain.getCountryOccurenecs().put(data.getString(0), data.getInt(1));
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
				tmp.add(new MailApproval(data.getString(1), data.getInt(0)));
			}
			
			stmt.close();
	
			for(MailApproval domain : tmp)
			{
				data = conn.createStatement().executeQuery("SELECT accp_country_code FROM accp WHERE accp_domain_id="+domain.getDomainId());
				while(data.next())
				{
					domain.getCountryOccurenecs().put(data.getString(0), 0);
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
				ResultSet data = stmt.executeQuery("SELECT domain_id, domain_name, domain_country FROM domain");)
		{
			while(data.next())
				tmp.add(new Domain(data.getInt(0), data.getString(1), data.getString(2)));
			
			return tmp;
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
				
		
		return null;
	}
}
