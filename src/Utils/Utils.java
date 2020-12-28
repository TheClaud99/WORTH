package Utils;

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

}
