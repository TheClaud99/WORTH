import Utils.Notification;

import java.rmi.*;

public interface NotifyEventInterface extends Remote {
    /*
     * Metodo invocato dal server per notificare un evento ad un client remoto.
     */
    void notifyEvent(Notification notification) throws RemoteException;
}