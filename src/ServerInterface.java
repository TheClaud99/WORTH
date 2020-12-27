import java.rmi.*;

public interface ServerInterface extends Remote {
    /* registrazione per la callback */
    void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException;

    /* cancella registrazione per la callback */
    void unregisterForCallback(NotifyEventInterface ClientInterface) throws RemoteException;

    void register(String username, String password) throws RemoteException;
}