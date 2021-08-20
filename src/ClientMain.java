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

    private static String ServerAddress = "127.0.0.1";
    private final int BUFFER_DIMENSION = 1024;
    private static int RMIport = 5000; //RMI port
    private static int TCPport = 1919; //TCP port for connection
    private static int CHAT_PORT = 2000; //Chat UDP port
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
            if (!porjectChatIps.containsKey(chat.getKey())) {
                chat.getValue().interrupt();
                iterator.remove();
            }
        }
    }

    /*
     * metodo che può essere richiamato dal servente per notificare il login
     * di un'utente o un'aggiornamento della lista dei progetti di cui l'utente è membro
     */
    public synchronized void notifyEvent(Notification notification) throws RemoteException {
        this.users = notification.users;
        try {
            updateChats(notification.porjectChatIps);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public Response register(String username, String password) throws IOException, UserNotFoundException, ClassNotFoundException {

        if (logged) {
            return new Response(false, "Sei già loggato");
        }

        Response response = server.register(username, password);
        if (response.success)
            login(username, password);
        return response;
    }

    public Response login(String username, String password) throws IOException, UserNotFoundException, ClassNotFoundException {
        this.username = username;
        this.password = password;
        server.registerForCallback(this, username);
        sendCommand(String.format("login %s %s", username, password));
        Response response = getResponse();
        if (response.success) {
            logged = true;
        } else {
            server.unregisterForCallback(username);
        }

        return response;
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

    private synchronized void listUsers() {
        for (Map.Entry<String, Boolean> user : users.entrySet())
            if (user.getValue())
                System.out.println(user.getKey() + ": Online");
            else
                System.out.println(user.getKey() + ": Offline");
    }

    private synchronized void listOnlineUsers() {
        for (Map.Entry<String, Boolean> user : users.entrySet())
            if (user.getValue())
                System.out.println(user.getKey());
    }

    private synchronized void readChat(String chat) {
        if (!chatList.containsKey(chat)) {
            System.out.println("Chat non trovata");
            return;
        }
        for (String text : chatList.get(chat).readMessages())
            System.out.println(text);
    }

    private synchronized void sendChatMsg(String chat, String message) throws IOException {
        if (!chatList.containsKey(chat)) {
            System.out.println("Chat non trovata");
            return;
        }

        chatList.get(chat).sendMsg(this.username + ": " + message);
        System.out.println("message sent");
    }

    private void executeCommand(String command) throws IOException, ClassNotFoundException, UserNotFoundException, ArrayIndexOutOfBoundsException {
        String[] splittedCommand = command.split(" ");
        Response response;

        switch (splittedCommand[0].toLowerCase()) {

            case "listusers":
                listUsers();
                break;

            case "listonlineusers":
                listOnlineUsers();
                break;

            case "readchat":
                readChat(splittedCommand[1]);
                break;

            case "sendchatmsg":
                String message = command.split("\"")[1];
                sendChatMsg(splittedCommand[1], message);
                break;

            case "login":
                response = login(splittedCommand[1], splittedCommand[2]);
                System.out.printf("< %s\n", response.message);
                break;

            case "listprojects":
            case "showmembers":
            case "showcards":
            case "showcard":
            case "getcardhistory":
                sendCommand(command);
                response = getResponse();
                System.out.printf("< %s\n", response.message);
                if (response.success)
                    for (String text : response.list)
                        System.out.println(text);
                break;

            case "":
                break;

            case "help":
                help();
                break;


            case EXIT_CMD:
                this.exit = true;
                sendCommand(command);
                break;

            case "register":
                response = register(splittedCommand[1], splittedCommand[2]);
                System.out.printf("< %s\n", response.message);
                break;

            default:
                sendCommand(command);
                response = getResponse();
                System.out.printf("< %s\n", response.message);
                break;
        }
    }

    public void start() throws IOException {
        try {
            client = SocketChannel.open(new InetSocketAddress(ServerAddress, TCPport));
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Client: connesso");
            System.out.println("Digita '" + this.EXIT_CMD + "' per uscire");
            System.out.println("Digita 'help' per vedere la lista dei comandi disponibili");

            while (!this.exit) {
                System.out.print("> ");
                String msg = consoleReader.readLine().trim();

                try {
                    executeCommand(msg);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Missing arguments");
                } catch (UserNotFoundException e) {
                    System.out.println(e.getMessage());
                } catch (ConnectException e) {
                    System.out.println("Server irraggiungibile, digita '" + this.EXIT_CMD + "' per uscire");
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

    private void help() {
        System.out.println("**************** Comandi *******************");
        System.out.println("register [username] [password]      Registra nuovo utente");
        System.out.println("login [username] [password]         Effettua login");
        System.out.printf("%s                                 Effettua logout\n", this.EXIT_CMD);
        System.out.println("listusers                           Mostra lista utenti registrati");
        System.out.println("listonlineusers                     Mostra utenti attualmente online");
        System.out.println("createproject [project name]        Crea un nuovo progetto");
        System.out.println("listprojects                        Mostra lista progetti di cui fai parte");
        System.out.println("addmember [project name] [username] Aggiungi nuovo utente al progetto");
        System.out.println("showmembers [project name]          Mostra membri di un progetto");
        System.out.println("showcards [project name]            Mostra card di un progetto");
        System.out.println("showcard [project name] [card name] Mostra info di una card");
        System.out.println("addcard [project name] [card name] [card description] Aggiungi una card al progetto");
        System.out.println("movecard [project name] [card name] [old state] [new state] Sposta una card da uno stato ad un altro");
        System.out.println("getcardhistory [project name] [card name] Mostra la cronologia degli spostamenti di una card");
        System.out.println("readchat [project name]             Leggi messaggi nella chat di un progetto");
        System.out.println("sendchatmsg [project name] \"[message]\" Manda un messaggio nella chat di un progetto (Il messaggio deve essere racchiuso tra \")");
        System.out.println("cancelproject [project name]        Cancella un progetto (tutte le card devono essere done)");
    }

    public static void main(String[] args) throws RemoteException {
        ClientMain clientMain;

        try {
            if (args.length == 1) {
                ServerAddress = args[0];
            }

            if(args.length == 2) {
                ServerAddress = args[0];
                TCPport = Integer.parseInt(args[1]);
            }

            if(args.length == 3) {
                ServerAddress = args[0];
                TCPport = Integer.parseInt(args[1]);
                RMIport = Integer.parseInt(args[2]);
            }

            if(args.length == 4) {
                ServerAddress = args[0];
                TCPport = Integer.parseInt(args[1]);
                RMIport = Integer.parseInt(args[2]);
                CHAT_PORT = Integer.parseInt(args[3]);
            }

        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }


        try {
            Registry registry = LocateRegistry.getRegistry(RMIport);
            String name = "Server";
            ServerInterface server = (ServerInterface) registry.lookup(name);
            clientMain = new ClientMain(server);
            clientMain.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}