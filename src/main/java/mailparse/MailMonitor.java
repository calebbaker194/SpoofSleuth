package mailparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;

import org.apache.poi.poifs.filesystem.FileMagic;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import data.Database;
import mailalert.Notifier;
import mailobject.ImapServer;
import mailobject.MailAttachment;

public class MailMonitor {

    private IdleThread idleThread;
    
	public MailMonitor(ImapServer user) {
	
		System.out.println("Prepairing to monitor "+user.getAddress());
		
	    Properties properties = new Properties();
	    // properties.put("mail.debug", "true");
	    properties.put("mail.store.protocol", user.getImapProtocol());
	    properties.put("mail.imaps.host", user.getImapHost());
	    properties.put("mail.imaps.port", user.getImapPort());
	    properties.put("mail.imaps.timeout", "10000");
	
	    Session session = Session.getInstance(properties);
	    //session.setDebug(true);
	                                                       
	    IMAPStore store = null;
	    Folder inbox = null;
	
	    try {
	        store = (IMAPStore) session.getStore(user.getImapProtocol());
	        store.connect(user.getUsername(), user.getPassword());
	
	        if (!store.hasCapability("IDLE")) {
	            throw new RuntimeException("IDLE not supported");
	        }
	
	        inbox = (IMAPFolder) store.getFolder("INBOX");
	        inbox.addMessageCountListener(new MessageCountAdapter() {
	
	            @Override
	            public void messagesAdded(MessageCountEvent event) {
	                Message[] messages = event.getMessages();
	                
	                for (Message message : messages) 
	                {
	                    try // MAIN MESSAGE PARSE LOGIC
	                    {   
	                    	
	                    	String recSpf = message.getHeader("Received-SPF")[0];
	                    	
	                    	// IP Check
	                    	String client = recSpf.substring(recSpf.indexOf("client-ip=")+10).replaceAll("[\\s;]", "");
	                    	String countryCode = MailCentral.getCountryCode(client);
	                    	
	                    	// SPF Check
	                    	Scanner spfsc = new Scanner(recSpf);
	                    	String spfResult = spfsc.next();
	                    	spfsc.close();
	                    	
	                    	// Spoof Check
	                    	String returnPath = message.getHeader("Return-Path")[0];
	                    	String returnPathAddress = "";
	                    	try 
	                    	{
	                    		returnPathAddress = returnPath.substring(returnPath.indexOf('<')+1,returnPath.indexOf('>'));
	                    	}
	                    	catch(StringIndexOutOfBoundsException err)
	                    	{
	                    		returnPathAddress = returnPath;
	                    	}
	                    	String fromField = message.getFrom()[0].toString();
	                    	String fromFieldAddress = "";
	                    	try 
	                    	{
	                    		fromFieldAddress = fromField.substring(fromField.indexOf('<')+1,fromField.indexOf('>'));
	                    	}
	                    	catch(StringIndexOutOfBoundsException err)
	                    	{
	                    		fromFieldAddress = fromField;
	                    	}
	                    	
	                    	String domain = fromFieldAddress.substring(fromFieldAddress.indexOf('@')+1);
	                    	
	                    	// Macro Check
	                    	int macroCount = 0;
	                    	ArrayList<MailAttachment> attachments = MailCentral.getAttachments(message);
	                    	ArrayList<MailAttachment> corrupt = new ArrayList<MailAttachment>();
	                    	if(attachments != null)
	                    	{
		                    	for(MailAttachment st : attachments)
		                    	{
		                    		if(FileMagic.valueOf(st.getAttachment()) == FileMagic.OLE2 || FileMagic.valueOf(st.getAttachment()) ==  FileMagic.OOXML)
		                    		{
		                    			if(PoiMaster.hasMacro(st.getAttachment()))
		                    			{
		                    				corrupt.add(st);
		                    				macroCount++;
		                    			}
		                    		}
		                    		else if(st.getName().substring(st.getName().lastIndexOf('.')).matches("(.exe)|(.zip)"))
		                    		{
		                    			corrupt.add(st);
		                    			macroCount++;
		                    		}
		                    	}
	                    	}
	                    	
	                    	//TODO - Phishing Check 
	                    	
	                    	// Notify Results
	                    	int check=0;
	                    	
	                    	check+= Database.checkApprovedDomain(countryCode, domain) ? 0 : 1;
	                    	check+= spfResult.equalsIgnoreCase("pass") || spfResult.equalsIgnoreCase("softfail") || spfResult.equalsIgnoreCase("neutral") || spfResult.equalsIgnoreCase("none") ? 0 : 2;
	                    	check+= checkDomainSpoof(fromFieldAddress, returnPath, domain) ? 0 : 4;
	                    	check+= macroCount > 0 ? 8 : 0 ;
	                    	
	                    	if(check != 0)
	                    	{
	                    		HashMap<String, String> tmp = new HashMap<String, String>();
	                    		tmp.put("domain", domain);
	                    		tmp.put("spfResult", spfResult);
	                    		tmp.put("countryCode", countryCode);
	                    		tmp.put("returnPath", returnPathAddress);
	                    		tmp.put("from", fromFieldAddress);
	                    		
	                    		String recp = "";
	                    		for(Address s : message.getAllRecipients())
	                    		{
	                    			recp += s.toString()+",";
	                    		}
	                    		
	                    		tmp.put("to", recp.substring(0,recp.length()-1));
	                    		tmp.put("subject", message.getSubject());
	                    		tmp.put("body", message.getContent().toString());
	                    		tmp.put("macroCount",""+macroCount);
	                    		
	                    		for(int x=0; x<macroCount; x++)
	                    		{
	                    			tmp.put("infected"+x,corrupt.get(x).getName());
	                    		}
	                    		
	                    		Notifier.notify(check, tmp);
	                    	}
	                    	
	                    }
	                    catch (FolderClosedException e)
	                    {
	                    	System.out.println("Folder Closed Attemting to reopen");
	                    }
	                    catch (Exception e) 
	                    {
	                        e.printStackTrace();
	                    }
	                }
	            }

				private boolean checkDomainSpoof(String fromFieldAddress, String returnPath, String domain)
				{
					return !(returnPath.equals(fromFieldAddress)
						|| returnPath.matches("^.*[\\.@]"+domain+"$"));
				}
	        });
	
	        idleThread = new IdleThread(inbox, user);
	        idleThread.setDaemon(false);
	        
	
	    } catch (AuthenticationFailedException e) {
	    	System.out.println("Authentication Failed");
	    	System.out.println("Username:" +user.getUsername()+ "\nPassword: " + user.getPassword());
	        e.printStackTrace();
	    } catch (NoSuchProviderException e)
		{
			e.printStackTrace();
		} catch (MessagingException e)
		{
			e.printStackTrace();
		} finally {
	
	        close(inbox);
	        close(store);
	    }
	}

	public IdleThread getIdleThread() {
		return idleThread;
	}
	
    public static class IdleThread extends Thread {
        private final Folder folder;
        private volatile boolean running = true;
        private ImapServer user;

        public IdleThread(Folder folder, ImapServer user) {
            super();
            this.folder = folder;
            this.user = user;
        }

        public synchronized void kill() {
        	
            if (!running)
                return;
            this.running = false;
        }

        @Override
        public void run() {
            while (running) {

                try {
                    ensureOpen(folder, user);
                    ((IMAPFolder) folder).idle();
                }
                catch (FolderClosedException e) {
                	System.out.println("Folder Closed. Re-opening "+folder.getName()+" For "+user.getAddress());
                }
                catch (Exception e) {
                    // something went wrong
                    // wait and try again
                    e.printStackTrace();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        // ignore
                    }
                }

            }
        }
    }

    public static void close(final Folder folder) {
        try {
            if (folder != null && folder.isOpen()) {
                folder.close(false);
            }
        } catch (final Exception e) {
            // ignore
        }

    }

    public static void close(final Store store) {
        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (final Exception e) {
            // ignore
        }

    }

    public static void ensureOpen(final Folder folder, ImapServer user) throws MessagingException {

        if (folder != null) {
            Store store = folder.getStore();
            if (store != null && !store.isConnected()) {
                store.connect(user.getUsername(), user.getPassword());
            }
        } else {
            throw new MessagingException("Unable to open a null folder");
        }

        if (folder.exists() && !folder.isOpen() && (folder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            System.out.println("open " + user.getAddress() + " " + folder.getFullName());
            folder.open(Folder.READ_ONLY);
            if (!folder.isOpen())
                throw new MessagingException("Unable to open folder " + folder.getFullName());
        }

    }
}