import Exceptions.CardNotFoundException;
import Exceptions.IllegalMoveException;
import Exceptions.ProjectNotFoundException;
import Exceptions.UserNotFoundException;
import Utils.Response;
import Utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
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
    private Projects projects;

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
        storage = new StorageManager(storageDir, usersFilePath, projecstDir);
        users = new Users(storage);
        projects = new Projects(storage);
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
                if (!command.equals(this.EXIT_CMD)) key.interestOps(SelectionKey.OP_WRITE);
            } else if (key.isWritable()) { // WRITABLE
                this.answerClient(sel, key);
            }
        }
    }

    private void executeCommand(String command, SelectionKey key) throws IOException {
        String[] splittedCommand = command.split(" ");
        System.out.println("Command requested: " + command);

        if (command.equals(this.EXIT_CMD)) {
            users.logout(key);
            key.channel().close();
            key.cancel();
            return;
        }

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
                if (users.isLogged(key))
                    key.attach(new Response(true, "Lista utenti:", users.getUsersList()));
                else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "listonlineusers":
                if (users.isLogged(key))
                    key.attach(new Response(true, "Lista utenti:", users.getOnlineUsersList()));
                else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "listprojects":
                if (users.isLogged(key))
                    key.attach(new Response(true, "Lista progetti:", projects.listProjects(users.getUsernameByKey(key)).toArray(new String[0])));
                else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "createproject":
                if (users.isLogged(key)) {
                    try {
                        projects.addProject(splittedCommand[1]);
                        projects.addMember(splittedCommand[1], users.getUsernameByKey(key));
                    } catch (IOException e) {
                        key.attach(new Response(false, "Server error"));
                    }
                    key.attach(new Response(true, "Creato progetto " + splittedCommand[1]));
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "addmember":
                if (users.isLogged(key)) {
                    try {
                        Project project = projects.getByName(splittedCommand[1]);
                        if (!project.isMember(users.getUsernameByKey(key))) {
                            key.attach(new Response(false, "Non sei membro del progetto"));
                        } else {
                            project.addMember(splittedCommand[2]);
                            projects.updateProjects();
                            key.attach(new Response(true, "Aggiunto utente " + splittedCommand[2] + " a progetto " + splittedCommand[1]));
                        }
                    } catch (ProjectNotFoundException e) {
                        key.attach(new Response(false, "Progetto non trovato"));
                    } catch (IOException e) {
                        key.attach(new Response(false, "Server error"));
                    }
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "showmembers":
                if (users.isLogged(key)) {
                    try {
                        Project project = projects.getByName(splittedCommand[1]);
                        if (!project.isMember(users.getUsernameByKey(key))) {
                            key.attach(new Response(false, "Non sei membro del progetto"));
                        } else {
                            String[] members = project.getMembers().toArray(new String[0]);
                            for(String member : members)
                                System.out.println(member);
                            key.attach(new Response(true, "Membri:", members));
                        }
                    } catch (ProjectNotFoundException e) {
                        key.attach(new Response(false, "Progetto non trovato"));
                    }
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "showcards":
                if (users.isLogged(key)) {
                    try {
                        Project project = projects.getByName(splittedCommand[1]);
                        if (!project.isMember(users.getUsernameByKey(key))) {
                            key.attach(new Response(false, "Non sei membro del progetto"));
                        } else {
                            key.attach(new Response(true, "Membri: \n", project.getCardsList().toArray(new String[0])));
                        }
                    } catch (ProjectNotFoundException e) {
                        key.attach(new Response(false, "Progetto non trovato"));
                    }
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "showcard":
                if (users.isLogged(key)) {
                    try {
                        Project project = projects.getByName(splittedCommand[1]);
                        if (!project.isMember(users.getUsernameByKey(key))) {
                            key.attach(new Response(false, "Non sei membro del progetto"));
                        } else {
                            key.attach(new Response(true, "Info card: \n", project.getCardInfo(splittedCommand[2]).toArray(new String[0])));
                        }
                    } catch (ProjectNotFoundException e) {
                        key.attach(new Response(false, "Progetto non trovato"));
                    } catch (CardNotFoundException e) {
                        key.attach(new Response(false, "Card non trovata"));
                    }
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "addcard":
                if (users.isLogged(key)) {
                    try {
                        Project project = projects.getByName(splittedCommand[1]);
                        if (!project.isMember(users.getUsernameByKey(key))) {
                            key.attach(new Response(false, "Non sei membro del progetto"));
                        } else {
                            project.createCard(splittedCommand[2], splittedCommand[3]);
                            projects.updateProjects();
                            key.attach(new Response(true, "Aggiunta card " + splittedCommand[2] + " a progetto " + splittedCommand[1]));
                        }
                    } catch (ProjectNotFoundException e) {
                        key.attach(new Response(false, "Progetto non trovato"));
                    } catch (IOException e) {
                        key.attach(new Response(false, "Server error"));
                    }
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;


            case "movecard":
                if (users.isLogged(key)) {
                    try {
                        Project project = projects.getByName(splittedCommand[1]);
                        if (!project.isMember(users.getUsernameByKey(key))) {
                            key.attach(new Response(false, "Non sei membro del progetto"));
                        } else {
                            project.moveCard(splittedCommand[2], splittedCommand[3], splittedCommand[4]);
                            projects.updateProjects();
                            key.attach(new Response(true, "Spostamento avvenuto con successo"));
                        }
                    } catch (ProjectNotFoundException e) {
                        key.attach(new Response(false, "Progetto non trovato"));
                    } catch (CardNotFoundException e) {
                        key.attach(new Response(false, "Card non trovata"));
                    } catch (IllegalMoveException e) {
                        key.attach(new Response(false, "Spostamento non consentito"));
                    } catch (IOException e) {
                        key.attach(new Response(false, "Server error"));
                    }
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "getcardhistory":
                if (users.isLogged(key)) {
                    try {
                        Project project = projects.getByName(splittedCommand[1]);
                        if (!project.isMember(users.getUsernameByKey(key))) {
                            key.attach(new Response(false, "Non sei membro del progetto"));
                        } else {
                            key.attach(new Response(true, "Cronologia card: \n", project.getCardHistory(splittedCommand[2]).toArray(new String[0])));
                        }
                    } catch (ProjectNotFoundException e) {
                        key.attach(new Response(false, "Progetto non trovato"));
                    } catch (CardNotFoundException e) {
                        key.attach(new Response(false, "Card non trovata"));
                    }
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "cancelproject":
                if (users.isLogged(key)) {
                    try {
                        Project project = projects.getByName(splittedCommand[1]);
                        if (!project.isMember(users.getUsernameByKey(key))) {
                            key.attach(new Response(false, "Non sei membro del progetto"));
                        } else {
                            projects.deleteProject(project);
                            key.attach(new Response(true, "Eliminato progetto " + splittedCommand[1]));
                        }
                    } catch (ProjectNotFoundException e) {
                        key.attach(new Response(false, "Progetto non trovato"));
                    } catch (IOException e) {
                        key.attach(new Response(false, "Server error"));
                    }
                } else
                    key.attach(new Response(false, "Non sei loggato"));
                break;

            case "readchat":
                break;

            case "sendchatmsg":
                break;

            case "":
            case "exit":
                break;

            default:
                key.attach(new Response(false, "Comando non trovato"));
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
            e.printStackTrace();
        }
    }
}