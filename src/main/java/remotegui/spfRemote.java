package remotegui;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface spfRemote extends Remote{
	public void openUI() throws RemoteException;
}
