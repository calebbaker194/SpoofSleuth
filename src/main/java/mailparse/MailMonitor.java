package mailparse;

import java.util.Properties;

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

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import mailalert.Notifier;
import mailobject.ImapServer;

public class MailMonitor {

    private IdleThread idleThread;
    
	public MailMonitor(ImapServer user) {
	
	    Properties properties = new Properties();
	    // properties.put("mail.debug", "true");
	    properties.put("mail.store.protocol", user.getImapProtocol());
	    properties.put("mail.imaps.host", user.getImapHost());
	    properties.put("mail.imaps.port", user.getImapPort());
	    properties.put("mail.imaps.timeout", "10000");
	
	    Session session = Session.getInstance(properties);
	                                                       
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
	                    	System.out.println("Recieved Message From "+ message.getFrom()[0].toString());
	                    	String client = message.getHeader("Received-SPF")[0].split("\\n")[6];
	                    	String countryCode = MailCentral.getCountryCode(client.substring(client.indexOf("=")+1));
	                    	if(! ( countryCode.equals("US") || countryCode.equals("LOCAL") ))
	                    	{
	                    		Notifier.notify("it@pittsburgsteel.com",message);
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
	        });
	
	        idleThread = new IdleThread(inbox, user);
	        idleThread.setDaemon(false);
	        //idleThread.start();
	
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
                } catch (Exception e) {
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