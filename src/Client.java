import RMI.*;
import java.rmi.registry.*;
import java.rmi.server.*;

public class Client {
    public static void main(String[] args) {
        try {
            System.out.println("Cerco il Server");
            Registry registry = LocateRegistry.getRegistry(5000);
            String name = "Server";
            ServerInterface server = (ServerInterface) registry.lookup(name);
            /* si registra per la callback */
            System.out.println("Registering for callback");
            NotifyEventInterface callbackObj = new NotifyEventImpl();
            NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
            server.registerForCallback(stub);
            // attende gli eventi generati dal server per
            // un certo intervallo di tempo;
            Thread.sleep(20000);
            /* cancella la registrazione per la callback */
            System.out.println("Unregistering for callback");
            server.unregisterForCallback(stub);
        } catch (Exception e) {
            System.err.println("Client exception:" + e.getMessage());
        }
    }
}
