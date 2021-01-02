import Exceptions.UserNotFoundException;
import Utils.Response;
import Utils.Utils;
import Utils.Notification;
import Utils.ChatThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ClientMain extends UnicastRemoteObject implements NotifyEventInterface {
    /**
     *
     */
    private static final long serialVersionUID = 5466266430079395311L;

    private final ServerInterface server;
    private String username;
    private String password;
    private boolean logged;

    private final Map<String, ChatThread> chatList;
    private Map<String, Boolean> users;

    private static final String ServerAddress = "127.0.0.1";
    private final int BUFFER_DIMENSION = 1024;
    private static final int RMIport = 5000; //RMI port
    private static final int TCPport = 1919; //TCP port for connection
    private static final int CHAT_PORT = 2000; //Chat UDP port
    private final String EXIT_CMD = "exit";
    private SocketChannel client;

    private boolean exit;

    /* crea un nuovo callback client */
    public ClientMain(ServerInterface server) throws RemoteException {
        super();
        this.users = new HashMap<>();
        this.chatList = new HashMap<>();
        this.server = server;
    }

    private void updateChats(Map<String, String> porjectChatIps) throws IOException {

        // Crea nuove chat di evenutiali nuovi progetti
        for (Map.Entry<String, String> chat : porjectChatIps.entrySet()) {
            String project = chat.getKey();
            String address = chat.getValue();
            if (!chatList.containsKey(project)) {
                ChatThread chatThread = new ChatThread(address, CHAT_PORT);
                chatList.put(project, chatThread);
                chatThread.start();
            }
        }

        // Elimina le chat dei progetti eliminati
        Iterator<Entry<String, ChatThread>> iterator = chatList.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, ChatThread> chat = iterator.next();
            if(!porjectChatIps.containsKey(chat.getKey())) {
                chat.getValue().interrupt();
                iterator.remove();
            }
        }
    }

    /*
     * metodo che può essere richiamato dal servente per notificare una nuova
     * quotazione del titolo
     */
    public void notifyEvent(Notification notification) throws RemoteException {
        this.users = notification.users;
        try {
            updateChats(notification.porjectChatIps);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public Response register(String username, String password) throws IOException, UserNotFoundException, ClassNotFoundException {
        Response response = server.register(username, password);
        login(username, password);
        return response;
    }

    public void login(String username, String password) throws IOException, UserNotFoundException, ClassNotFoundException {
        this.username = username;
        this.password = password;
        server.registerForCallback(this, username);
        sendCommand(String.format("login %s %s", username, password));
        Response response = getResponse();
        if (response.success) {
            System.out.println("Login avvenuto con successo");
            logged = true;
        } else
            server.unregisterForCallback(username);
    }

    public void close() {
        try {
            if (logged)
                server.unregisterForCallback(this.username);
        } catch (RemoteException | UserNotFoundException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    private void sendCommand(String command) throws IOException {
        // Creo il messaggio da inviare al server
        ByteBuffer readBuffer = ByteBuffer.wrap(command.getBytes());
        client.write(readBuffer);
        readBuffer.clear();
    }

    private Response getResponse() throws IOException, ClassNotFoundException {
        ByteBuffer reply = ByteBuffer.allocate(BUFFER_DIMENSION);
        client.read(reply);
        reply.flip();
        Response response = (Response) Utils.deserialize(reply.array());
        reply.clear();
        return response;
    }

    private void executeCommand(String command) throws IOException, ClassNotFoundException, UserNotFoundException, ArrayIndexOutOfBoundsException {
        String[] splittedCommand = command.split(" ");
        Response response;

        switch (splittedCommand[0].toLowerCase()) {

            case "listusers":
                for (Map.Entry<String, Boolean> user : users.entrySet())
                    if (user.getValue())
                        System.out.println(user.getKey() + ": Online");
                    else
                        System.out.println(user.getKey() + ": Offline");
                break;

            case "listonlineusers":
                for (Map.Entry<String, Boolean> user : users.entrySet())
                    if (user.getValue())
                        System.out.println(user.getKey());
                break;

            case "readchat":
                if(!chatList.containsKey(splittedCommand[1])) {
                    System.out.println("Chat non trovata");
                    break;
                }
                for (String text : chatList.get(splittedCommand[1]).readMessages())
                    System.out.println(text);
                break;

            case "sendchatmsg":
                if(!chatList.containsKey(splittedCommand[1])) {
                    System.out.println("Chat non trovata");
                    break;
                }
                String message = command.split("\"")[1];
                chatList.get(splittedCommand[1]).sendMsg(this.username + ": " + message);
                System.out.println("message sent");
                break;

            case "login":
                login(splittedCommand[1], splittedCommand[2]);
                break;
            case "listprojects":
            case "showmembers":
            case "showcards":
            case "showcard":
            case "getcardhistory":
                sendCommand(command);
                response = getResponse();
                System.out.printf("Risposta server: \n\n%s\n", response.message);
                if (response.success)
                    for (String text : response.list)
                        System.out.println(text);
                break;

            case "":
                break;

            case EXIT_CMD:
                this.exit = true;
                sendCommand(command);
                break;

            default:
                sendCommand(command);
                response = getResponse();
                System.out.printf("Risposta server: %s\n", response.message);
                break;
        }
    }

    public void start() throws IOException {
        try {
            client = SocketChannel.open(new InetSocketAddress(ServerAddress, TCPport));
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Client: connesso");
            System.out.println("Digita 'exit' per uscire, i messaggi scritti saranno inviati al server:");

            while (!this.exit) {

                String msg = consoleReader.readLine().trim();

                try {
                    executeCommand(msg);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Missing arguments");
                } catch (UserNotFoundException e) {
                    System.out.println(e.getMessage());
                }
            }
            System.out.println("Client: chiusura");
            close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            client.close();
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