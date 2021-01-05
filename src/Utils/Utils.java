package Utils;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static byte[] base64ToByte(String str) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(str);
    }

    public static String byteToBase64(byte[] bt) {
        BASE64Encoder endecoder = new BASE64Encoder();
        return endecoder.encode(bt);
    }

    public static String sha512(String message, String saltKey) {
        MessageDigest digest = null;
        byte[] salt = new byte[0];

        try {
            salt = base64ToByte(saltKey);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        assert digest != null;
        digest.reset();
        digest.update(salt);

        byte[] btPass = digest.digest(message.getBytes(StandardCharsets.UTF_8));
        digest.reset();
        btPass = digest.digest(btPass);

        return byteToBase64(btPass);
    }
}
