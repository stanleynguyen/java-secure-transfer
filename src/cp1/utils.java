package cp1;


import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Created by vivek on 9/4/17.
 */
public class utils {
    public static void sendMessage(PrintWriter stringOut, String message, int Identity) {
        System.out.println("sending message");
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
        System.out.println("getting message");
        String response = stringIn.readLine();
        System.out.println(response);
        System.out.println("get message");
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
        System.out.println("geting bytes");
        String length = stringIn.readLine();
        System.out.println(length);
        int byteLength = Integer.parseInt(length);
        byte[] byteArray = new byte[byteLength];
        int offset = 0;
        int numRead = 0;
        while (offset < byteArray.length && (numRead = byteIn.read(byteArray, offset, byteArray.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < byteArray.length) {
            System.out.println("File reception incomplete!");
        }
        System.out.println("got bytes");
        return byteArray;
    }

    public static void sendBytes(byte[] byteArray, PrintWriter stringOut,OutputStream byteOut) throws IOException {
        System.out.println("sending bytes");
        stringOut.println(byteArray.length);
        stringOut.flush();
        byteOut.write(byteArray, 0, byteArray.length);
        byteOut.flush();
        System.out.println("sent bytes");
    }
}
