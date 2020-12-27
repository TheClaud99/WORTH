import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

public class ClientMain extends RemoteObject implements NotifyEventInterface {
    /**
     *
     */
    private static final long serialVersionUID = 5466266430079395311L;

    private final ServerInterface server;

    /* crea un nuovo callback client */
    public ClientMain(ServerInterface server) throws RemoteException {
        super();
        this.server = server;
    }

    /*
     * metodo che può essere richiamato dal servente per notificare una nuova
     * quotazione del titolo
     */
    public void notifyEvent(int value) throws RemoteException {
        String returnMessage = "Update event received: " + value;
        System.out.println(returnMessage);
    }

    public void register(String username, String password) throws RemoteException {
        server.register(username, password);
    }

    public void close() {
        try {
            server.unregisterForCallback(this);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    public static void main(String[] args) throws RemoteException {
        ClientMain clientMain;
        RegisterForm form;
        try {
            Registry registry = LocateRegistry.getRegistry(5000);
            String name = "Server";
            ServerInterface server = (ServerInterface) registry.lookup(name);
            clientMain = new ClientMain(server);
            form = new RegisterForm(clientMain);
            NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(clientMain, 0);
            server.registerForCallback(stub);
            form.setVisible(true);
        } catch (Exception e) {
            System.err.println("Client exception:" + e.getMessage());
        }
    }
}