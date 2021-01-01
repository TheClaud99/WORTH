import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ChatThread extends Thread {
    /*final static int BUFFER_SIZE =4096;
    private MessagingQueue queue;
    private String IP;
    private int PORT;
    private MulticastSocket multicast;

    public ChatThread(MessagingQueue queue, String IP, int PORT) throws IOException
    {
        this.queue=queue;
        this.IP=IP;
        this.PORT=PORT;
        multicast =new MulticastSocket(this.PORT);
        InetAddress group = InetAddress.getByName(this.IP);
        multicast.joinGroup(group);
    }

    public void addMessage(String msg)
    {
        queue.put(msg);
    }

    public void recive() throws IOException
    {
        byte[] msg_recived= new byte[BUFFER_SIZE];
        DatagramPacket PACK = new DatagramPacket(msg_recived, msg_recived.length);
        multicast.receive(PACK);
        String msg = new String(PACK.getData(),0,PACK.getLength(),"UTF-8");
        addMessage(msg);
    }
    public void send() throws IOException
    {
        byte[] msg_send = queue.getMsg().getBytes();
        DatagramPacket PACK = new DatagramPacket(msg_send, msg_send.length);
        multicast.send(PACK);
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted())
            {
                if(!queue.isSended()) send();
                recive();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        multicast.close();
    }*/
    
}
