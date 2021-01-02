import Exceptions.UserNotFoundException;
import Utils.Response;
import Utils.Utils;
import Utils.Notification;

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
import java.util.Arrays;
import java.util.Map;

public class ClientMain extends UnicastRemoteObject implements NotifyEventInterface {
    /**
     *
     */
    private static final long serialVersionUID = 5466266430079395311L;

    private final ServerInterface server;
    private String username;
    private String password;

    private Map<String, String> chatList;
    private String[] users;

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
    public void notifyEvent(Notification notification) throws RemoteException {
        this.users = notification.users;
        this.chatList = notification.porjectChatIps;
    }

    public Response register(String username, String password) throws RemoteException, UserNotFoundException {
        Response response = server.register(username, password);
        login(username, password);
        return response;
    }

    public void login(String username, String password) throws RemoteException, UserNotFoundException {
        this.username = username;
        this.password = password;
//         NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(this, 0);
        server.registerForCallback(this, username);
    }

    public void close() {
        try {
            server.unregisterForCallback(this.username);
        } catch (RemoteException | UserNotFoundException e) {
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
                String command = msg.split(" ")[0];

                if(command.equals("login")) {
                    login(msg.split(" ")[1], msg.split(" ")[2]);
                }

                // Creo il messaggio da inviare al server
                ByteBuffer readBuffer = ByteBuffer.wrap(msg.getBytes());

                client.write(readBuffer);
                readBuffer.clear();

                if (msg.equals(this.EXIT_CMD)) {
                    this.exit = true;
                    continue;
                }

                ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIMENSION);
                client.read(reply);
                reply.flip();
                Response response = (Response) Utils.deserialize(reply.array());
                System.out.printf("Risposta server: %s\n", response.message);
                reply.clear();

                if (response.success) {
                    if (command.equalsIgnoreCase("listusers") || command.equalsIgnoreCase("listonlineusers")
                            || command.equalsIgnoreCase("listprojects")
                            || command.equalsIgnoreCase("showmembers")
                            || command.equalsIgnoreCase("showcards")
                            || command.equalsIgnoreCase("showcard")
                            || command.equalsIgnoreCase("getcardhistory")) {
                        for (String text : response.list) {
                            System.out.println(text);
                        }
                    }
                }


            }
            System.out.println("Client: chiusura");
            close();
        } catch (IOException | ClassNotFoundException | UserNotFoundException e) {
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
            // form = new RegisterForm(clientMain);
            // form.setVisible(true);
            clientMain.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}