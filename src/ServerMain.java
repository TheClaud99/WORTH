import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.*;

public class ServerMain extends RemoteObject implements ServerInterface {
    /**
     *
     */
    private static final long serialVersionUID = 150859790584022983L;
    private List<NotifyEventInterface> clients;

    /* crea un nuovo servente */
    public ServerMain() throws RemoteException {
        super();
        clients = new ArrayList<>();
    }

    public synchronized void registerForCallback(NotifyEventInterface ClientInterface) throws RemoteException {
        if (!clients.contains(ClientInterface)) {
            clients.add(ClientInterface);
            System.out.println("New client registered.");
        }
    }

    /* annulla registrazione per il callback */
    public synchronized void unregisterForCallback(NotifyEventInterface Client) throws RemoteException {
        if (clients.remove(Client)) {
            System.out.println("Client unregistered");
        } else {
            System.out.println("Unable to unregister client.");
        }
    }

    @Override
    public void register(String username, String password) throws RemoteException {
        System.out.printf("Username: %s \n Password: %s \n", username, password);
    }

    /*
     * notifica di una variazione di valore dell'azione /* quando viene richiamato,
     * fa il callback a tutti i client registrati
     */
    public void update(int value) throws RemoteException {
        doCallbacks(value);
    }

    private synchronized void doCallbacks(int value) throws RemoteException {
        System.out.println("Starting callbacks.");
        for (NotifyEventInterface client : clients) {
            client.notifyEvent(value);
        }
        System.out.println("Callbacks complete.");
    }

    public static void main(String[] args) {
        try {
            ServerMain serverMain = new ServerMain();
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(serverMain, 39000);
            String name = "Server";
            LocateRegistry.createRegistry(5000);
            Registry registry = LocateRegistry.getRegistry(5000);
            registry.bind(name, stub);
        } catch (Exception e) {
            System.out.println("Eccezione" + e);
        }
    }
}