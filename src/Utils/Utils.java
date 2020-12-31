package Utils;

import java.io.*;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {

    /*
     * Convert a 4-element array into dotted decimal format
     */
    private static String format(final int[] octets) {
        final StringBuilder str = new StringBuilder();
        for (int i =0; i < octets.length; ++i){
            str.append(octets[i]);
            if (i != octets.length - 1) {
                str.append(".");
            }
        }
        return str.toString();
    }

    public static String randomMutlticastIpv4() {
        int[] ip = new int[4];
        ip[0] = ThreadLocalRandom.current().nextInt(224, 240);
        ip[1] = ThreadLocalRandom.current().nextInt(0, 256);
        ip[2] = ThreadLocalRandom.current().nextInt(0, 256);
        ip[3] = ThreadLocalRandom.current().nextInt(0, 256);
        return format(ip);
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

}
