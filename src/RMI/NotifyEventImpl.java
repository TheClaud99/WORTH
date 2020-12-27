package RMI;

import java.rmi.*;
import java.rmi.server.*;

public class NotifyEventImpl extends RemoteObject implements NotifyEventInterface {
    /**
     *
     */
    private static final long serialVersionUID = 5466266430079395311L;

    /* crea un nuovo callback client */
    public NotifyEventImpl() throws RemoteException {
        super();
    }

    /*
     * metodo che può essere richiamato dal servente per notificare una nuova
     * quotazione del titolo
     */
    public void notifyEvent(int value) throws RemoteException {
        String returnMessage = "Update event received: " + value;
        System.out.println(returnMessage);
    }
}