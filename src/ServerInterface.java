import Exceptions.UserNotFoundException;
import Utils.Response;

import java.rmi.*;

public interface ServerInterface extends Remote {
    /* registrazione per la callback */
    void registerForCallback(NotifyEventInterface ClientInterface, String usrname) throws RemoteException, UserNotFoundException;

    /* cancella registrazione per la callback */
    void unregisterForCallback(String username) throws RemoteException, UserNotFoundException;

    Response register(String username, String password) throws RemoteException;
}