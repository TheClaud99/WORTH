import Utils.Response;
import Utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
    private Users users;

    /**
     * dimensione del buffer utilizzato per la lettura
     */
    private final int BUFFER_DIMENSION = 1024;
    /**
     * comando utilizzato dal client per comunicare la fine della comunicazione
     */
    private final String EXIT_CMD = "exit";
    /**
     * porta su cui aprire il listening socket
     */
    private static final int RMIport = 5000; //RMI port
    private static final int TCPport = 1919; //TCP port for connection
    /**
     * messaggio di risposta
     */
    private final String ADD_ANSWER = "echoed by server";
    /**
     * numero di client con i quali è aperta una connessione
     */
    public int numberActiveConnections;

    /**
     * Storage informations
     */
    private final String storageDir = "./storage";
    private final String usersFilePath = storageDir + "/users.json";
    private final String projecstDir = storageDir + "/projects";
    private final String cardFile = projecstDir + "/card_%s.json";

    private final ObjectMapper mapper;

    private StorageManager storage;

    /* crea un nuovo servente */
    public ServerMain() throws IOException {
        super();
        clients = new ArrayList<>();
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        storage = new StorageManager(storageDir, usersFilePath, cardFile);
        users = new Users(storage);
        restore();
    }

    public void restore() {
        /*try {
        } catch (IOException e) {
            e.printStackTrace();
        }*/
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
    public Response register(String username, String password) throws RemoteException {
        System.out.printf("Username: %s \n Password: %s \n", username, password);

        try {
            boolean succsess = users.register(username, password);
            if (succsess) {
                return new Response(true, "Registrazione avvenuta con successo");
            } else {
                return new Response(false, "Utente già presente");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new Response(false, e.toString());
        }
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

    /**
     * avvia l'esecuzione del server
     */
    public void start() {
        this.numberActiveConnections = 0;
        try (ServerSocketChannel s_channel = ServerSocketChannel.open()) {
            s_channel.socket().bind(new InetSocketAddress(TCPport));
            s_channel.configureBlocking(false);
            Selector sel = Selector.open();
            s_channel.register(sel, SelectionKey.OP_ACCEPT);
            System.out.printf("Server: in attessa di connessioni sulla porta %d\n", TCPport);
            while (true) {
                iterateKeys(sel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void iterateKeys(Selector sel) throws IOException {
        if (sel.select() == 0)
            return;

        // insieme delle chiavi corrispondenti a canali pronti
        Set<SelectionKey> selectedKeys = sel.selectedKeys();
        // iteratore dell'insieme sopra definito
        Iterator<SelectionKey> iter = selectedKeys.iterator();
        while (iter.hasNext()) {
            SelectionKey key = iter.next();
            iter.remove();
            if (key.isAcceptable()) { // ACCETTABLE
                /*
                 * accetta una nuova connessione creando un SocketChannel per la comunicazione
                 * con il client che la richiede
                 */
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                SocketChannel c_channel = server.accept();
                c_channel.configureBlocking(false);
                System.out.println(
                        "Server: accettata nuova connessione dal client: " + c_channel.getRemoteAddress());
                System.out.printf("Server: numero di connessioni aperte: %d\n", ++this.numberActiveConnections);
                this.registerRead(sel, c_channel);
            } else if (key.isReadable()) { // READABLE
                String command = this.readClientMessage(sel, key);
                executeCommand(command, key);
                key.interestOps(SelectionKey.OP_WRITE);
            } else if (key.isWritable()) { // WRITABLE
                this.answerClient(sel, key);
            }
        }
    }

    private void executeCommand(String command, SelectionKey key) {
        String[] splittedCommand = command.split(" ");
        System.out.println("Command requested: " + command);
        switch (splittedCommand[0].toLowerCase()) {
            case "login":
                try {
                    boolean success = users.login(splittedCommand[1], splittedCommand[2], key);
                    if (success) {
                        key.attach(new Response(true, "Login avvenuto con successo"));
                    } else {
                        key.attach(new Response(false, "Password non corretta"));
                    }
                } catch (UserNotFoundException e) {
                    key.attach(new Response(false, "Utente non trovato"));
                }
            case "logout":
                break;

            case "listusers":
                if(users.isLogged(key))
                    key.attach(new Response(true, "Lista utenti:", users.getUsersList()));
                else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "listonlineusers":
                if(users.isLogged(key))
                    key.attach(new Response(true, "Lista utenti:", users.getOnlineUsersList()));
                else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "listprojects":
                break;

            case "createproject":
                break;

            case "addmember":
                break;

            case "showmembers":
                break;

            case "showcards":
                break;

            case "showcard":
                break;

            case "addcard":
                break;


            case "movecard":
                break;

            case "getcardhistory":
                break;

            case "cancelproject":
                break;

            case "readchat":
                break;

            case "sendchatmsg":
                break;

            case "":
            case "exit":
                break;

            default:
                break;
        }
    }

    /**
     * registra l'interesse all'operazione di READ sul selettore
     *
     * @param sel       selettore utilizzato dal server
     * @param c_channel socket channel relativo al client
     * @throws IOException se si verifica un errore di I/O
     */
    private void registerRead(Selector sel, SocketChannel c_channel) throws IOException {

        // crea il buffer
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_DIMENSION);
        // aggiunge il canale del client al selector con l'operazione OP_READ
        // e aggiunge l'array di bytebuffer [length, message] come attachment
        c_channel.register(sel, SelectionKey.OP_READ, buffer);
    }

    /**
     * legge il messaggio inviato dal client e registra l'interesse all'operazione
     * di WRITE sul selettore
     *
     * @param sel selettore utilizzato dal server
     * @param key chiave di selezione
     * @throws IOException se si verifica un errore di I/O
     */
    private String readClientMessage(Selector sel, SelectionKey key) throws IOException {
        /*
         * accetta una nuova connessione creando un SocketChannel per la comunicazione
         * con il client che la richiede
         */
        SocketChannel c_channel = (SocketChannel) key.channel();
        // recupera l'array di bytebuffer (attachment)
        ByteBuffer bfs = (ByteBuffer) key.attachment();
        c_channel.read(bfs);

        bfs.flip();
        String msg = new String(bfs.array()).trim();
        System.out.printf("Server: ricevuto %s\n", msg);
        if (msg.equals(this.EXIT_CMD)) {
            System.out.println("Server: chiusa la connessione con il client " + c_channel.getRemoteAddress());
            users.logout(key);
            c_channel.close();
            key.cancel();
        }

        return msg;
    }

    /**
     * scrive il buffer sul canale del client
     *
     * @param key chiave di selezione
     * @throws IOException se si verifica un errore di I/O
     */
    private void answerClient(Selector sel, SelectionKey key) throws IOException {
        SocketChannel c_channel = (SocketChannel) key.channel();
        Response response = (Response) key.attachment();
        byte[] res = Utils.serialize(response);
        ByteBuffer bbEchoAnsw = ByteBuffer.wrap(res);
        c_channel.write(bbEchoAnsw);
        System.out.println("Server: " + response.message + " inviato al client " + c_channel.getRemoteAddress());
        if (!bbEchoAnsw.hasRemaining()) {
            bbEchoAnsw.clear();
            this.registerRead(sel, c_channel);
        }
    }

    public static void main(String[] args) {
        try {
            ServerMain serverMain = new ServerMain();
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(serverMain, 39000);
            String name = "Server";
            LocateRegistry.createRegistry(RMIport);
            Registry registry = LocateRegistry.getRegistry(RMIport);
            registry.bind(name, stub);
            serverMain.start();
        } catch (Exception e) {
            System.out.println("Eccezione" + e);
        }
    }
}