package remotegui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.nio.channels.FileLock;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.JOptionPane;

import launch.Main;

public class RemotOpen extends UnicastRemoteObject implements spfRemote{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2806786938752216007L;
	public static RandomAccessFile ins;
	public static FileLock lock;
	private Main instance;
	
    public RemotOpen(Main mainObj) throws RemoteException {
        
        super(0);
        instance = mainObj;
        		
        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(1099); 
            System.out.println("java RMI registry created.");
        } catch (RemoteException e) {
            //do nothing, error means registry already exists
            System.out.println("java RMI registry already exists.");
        }
        
        try
		{
			Naming.rebind("//localhost/SpoofSleuth", this);
		} catch (MalformedURLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("PeerServer bound in registry");
    }

	@Override
	public void openUI()
	{
		instance.openUI();
	}
	
	public static void checkForProgramRunning()
	{
		if(!new File("data/process.lock").exists())
		{
			try
			{
				new File("data/process.lock").createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		try
		{
			ins = new RandomAccessFile ("data/process.lock", "rw");
			lock = ins.getChannel().tryLock();
			
			if(lock == null)
			{
				System.out.println("Proccess is already Running");
				int option = JOptionPane.showConfirmDialog(null,"The Process is already running would you like to show the running processs?","Process Running",JOptionPane.YES_NO_OPTION);

				if(option == JOptionPane.YES_OPTION)
				{
					System.out.println("Yes");
					try
					{
						spfRemote runningInst = (spfRemote)Naming.lookup("//localhost/SpoofSleuth");
						runningInst.openUI();
					} catch (NotBoundException e)
					{
						System.out.println("Service Not Bound?");
						e.printStackTrace();
					}
				}
				System.exit(0);
			}
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			System.out.println("Could Not Aquire Lock");
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Lock Aquired");
	}
	
}
