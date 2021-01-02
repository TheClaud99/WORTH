package Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ChatThread extends Thread {

    private final MessagingQueue queue;
    private final int port;
    private final MulticastSocket multicast;
    private final InetAddress group;
    private final static int BUFFER_SIZE = 4096;

    public ChatThread(String groupAddress, int port) throws IOException {
        this.port = port;
        queue = new MessagingQueue();
        this.multicast = new MulticastSocket(port);
        this.group = InetAddress.getByName(groupAddress);
    }

    public List<String> readMessages() {
        return queue.getAndClear();
    }

    public void sendMsg(String msg) throws NullPointerException, IOException {
        if (msg == null) throw new NullPointerException();
        byte[] buffer = msg.getBytes();
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length, this.group, this.port);
        multicast.send(datagram);
    }

    public void recive() throws IOException {
        byte[] msg_recived = new byte[BUFFER_SIZE];
        DatagramPacket PACK = new DatagramPacket(msg_recived, msg_recived.length, this.group, this.port);
        multicast.receive(PACK);
        String msg = new String(PACK.getData(), 0, PACK.getLength(), StandardCharsets.UTF_8);

        addMessage(msg);
    }


    public void addMessage(String msg) {
        queue.put(msg);
    }

    @Override
    public void run() {
        try {
            multicast.joinGroup(group);
            while (!Thread.interrupted()) {
                recive();
            }
            multicast.leaveGroup(group);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            multicast.close();
        }
    }

}
