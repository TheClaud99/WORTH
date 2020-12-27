import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

public class ClientMain extends RemoteObject implements NotifyEventInterface {
    /**
     *
     */
    private static final long serialVersionUID = 5466266430079395311L;

    /* crea un nuovo callback client */
    public ClientMain() throws RemoteException {
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

    public static void main(String[] args) throws RemoteException {
        ClientMain clientMain = new ClientMain();
        try {
            System.out.println("Cerco il Server");
            Registry registry = LocateRegistry.getRegistry(5000);
            String name = "Server";
            ServerInterface server = (ServerInterface) registry.lookup(name);
            /* si registra per la callback */
            System.out.println("Registering for callback");
            NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(clientMain, 0);
            server.registerForCallback(stub);
        } catch (Exception e) {
            System.err.println("Client exception:" + e.getMessage());
        }
    }
}