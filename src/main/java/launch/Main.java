package launch;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Stack;
import javax.swing.JFrame;

import data.Database;
import data.FetchNStore;
import mailobject.ImapServer;
import mailparse.MailCentral;
import mailparse.MailMonitor;
import remotegui.RemotOpen;

public class Main implements Runnable {
	
	private HashMap<String,ImapServer> emails = new HashMap<String,ImapServer>();
	private Stack<MailMonitor> monitors = new Stack<MailMonitor>();
	
	private boolean running = true;
	private boolean started = false;
	public static boolean saved = false;
	
	
	public Main() throws RemoteException 
	{	
		//Check for database and create one if it does not exists
		if(!Database.exists())
		{
			boolean success = Database.createDatabase();
			if(!success)
			{
				System.out.println("Could not Create Database");
				System.exit(0);
			}
		}
		
		//Check for lock file and lock it if another process hasnt already.
		RemotOpen.checkForProgramRunning();
		new RemotOpen(this);
		
		// Load in ipData to avoid over requesting;
		MailCentral.setParent(this);
		
		// Load all Mail accounts to check.
		emails = FetchNStore.loadEmailData();
		
		for(String email : emails.keySet())
		{
			MailMonitor m = new MailMonitor(emails.get(email));
			monitors.push(m);
		}

	}
	
	public void close() 
	{
		try
		{
			RemotOpen.lock.release();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		try
		{
			RemotOpen.ins.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		for(MailMonitor monitor : monitors) 
		{
			monitor.getIdleThread().kill();
			try
			{
				monitor.getIdleThread().join();
			} catch (InterruptedException e)
			{
				// Thread Failed to Join
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String args[])throws Exception 
	{
		Thread t1 = new Thread(new Main());
		t1.start();
	}
	
	@Override
	public void run()
	{
		while(running)
		{
			if(!started )
			{
				for(MailMonitor monitor: monitors)
				{
					monitor.getIdleThread().start();
				}
				started = !started;
			}
			if(!Main.saved)
			{
				MailCentral.saveIpData();
				Main.saved = true;
				try
				{
					Thread.sleep(300000);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			try
			{
				Thread.sleep(1000);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	public void openUI()
	{
		System.out.println("Open UI");
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setVisible(true);
		f.setLocationRelativeTo(null);
		f.setSize(500,500);
	}
}
