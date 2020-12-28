import Utils.Response;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;

public class ClientMain extends UnicastRemoteObject implements NotifyEventInterface {
    /**
     *
     */
    private static final long serialVersionUID = 5466266430079395311L;

    private final ServerInterface server;
    private String username;
    private String password;

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

    public Response register(String username, String password) throws RemoteException {
        Response response = server.register(username, password);
        login(username, password);
        return response;
    }

    public void login(String username, String password) throws RemoteException {
        this.username = username;
        this.password = password;
        // NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(this, 0);
        server.registerForCallback(this);
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
            form.setVisible(true);
        } catch (Exception e) {
            System.err.println("Client exception:" + e.getMessage());
        }
    }
}