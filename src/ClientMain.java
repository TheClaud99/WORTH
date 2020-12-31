import Utils.Response;
import Utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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

    private static final String ServerAddress = "127.0.0.1";
    private final int BUFFER_DIMENSION = 1024;
    private static final int RMIport = 5000; //RMI port
    private static final int TCPport = 1919; //TCP port for connection
    private final String EXIT_CMD = "exit";

    private boolean exit;

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

    public void start() {

        try (SocketChannel client = SocketChannel.open(new InetSocketAddress(ServerAddress, TCPport))) {
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Client: connesso");
            System.out.println("Digita 'exit' per uscire, i messaggi scritti saranno inviati al server:");

            while (!this.exit) {

                String msg = consoleReader.readLine().trim();

                if (msg.equals(this.EXIT_CMD)) {
                    this.exit = true;
                    continue;
                }

                // Creo il messaggio da inviare al server
                ByteBuffer readBuffer = ByteBuffer.wrap(msg.getBytes());

                client.write(readBuffer);
                readBuffer.clear();

                ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIMENSION);
                client.read(reply);
                reply.flip();
                Response response = (Response) Utils.deserialize(reply.array());
                System.out.printf("Client: il server ha inviato %s\n", response.message);
                reply.clear();

            }
            System.out.println("Client: chiusura");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws RemoteException {
        ClientMain clientMain;
        RegisterForm form;
        try {
            Registry registry = LocateRegistry.getRegistry(RMIport);
            String name = "Server";
            ServerInterface server = (ServerInterface) registry.lookup(name);
            clientMain = new ClientMain(server);
            form = new RegisterForm(clientMain);
            form.setVisible(true);
            clientMain.start();
        } catch (Exception e) {
            System.err.println("Client exception:" + e.getMessage());
        }
    }
}