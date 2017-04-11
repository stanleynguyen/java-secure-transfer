package cp1;


import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Created by vivek on 9/4/17.
 */
public class utils {
    public static void sendMessage(PrintWriter stringOut, String message, int Identity) {
        String to;
        String from;
        if (Identity == 0) {
            from = "SERVER";
            to = "CLIENT";
        } else {
            from = "CLIENT";
            to = "SERVER";
        }
        stringOut.println(from + ">> " + message );
        stringOut.flush();
        System.out.println("Sent to " + to + ": " + message);
    }

    public static String getMessage(BufferedReader stringIn) throws IOException {
        String response = stringIn.readLine();
        System.out.println(response);
        return response;
    }

    public static byte[] generateNonce() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] nonce = new byte[8];
        secureRandom.nextBytes(nonce);
        System.out.println("Generated a 64 bit nonce");
        return nonce;
    }

    public static byte[] getBytes(BufferedReader stringIn, InputStream byteIn) throws IOException {
        int byteLength = Integer.parseInt(stringIn.readLine());

        // something is wrong here
        byte[] byteArray = new byte[byteLength];
        int offset = 0;
        int numRead = 0;
        while (offset < byteArray.length && (numRead = byteIn.read(byteArray, offset, byteArray.length - offset)) >= 0) {
            offset += numRead;
            System.out.println(offset);
        }
        if (offset < byteArray.length) {
            System.out.println("File reception incomplete!");
        }
        return byteArray;
    }

    public static void sendBytes(byte[] byteArray, PrintWriter stringOut,OutputStream byteOut) throws IOException {
        stringOut.println(byteArray.length);
        stringOut.flush();
        byteOut.write(byteArray, 0, byteArray.length);
        byteOut.flush();
    }
}
