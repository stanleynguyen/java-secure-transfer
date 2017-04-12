package cp1;


import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Created by vivek on 9/4/17.
 */
public class utils {
    private static long start_time;

    public static double tic(){
        return start_time = System.nanoTime();
    }

    public static double toc(){
        return (System.nanoTime()-start_time)/1000000000.0;
    }

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

    public static byte[] getBytes(BufferedReader stringIn, InputStream byteIn, PrintWriter stringOut) throws IOException {
        int byteLength = Integer.parseInt(stringIn.readLine());

        byte[] byteArray = new byte[byteLength];
        int offset = 0;
        int numRead = 0;
        stringOut.println("ready"); // signal sender I'm ready to get bytes
        stringOut.flush();
        while (offset < byteArray.length && (numRead = byteIn.read(byteArray, offset, byteArray.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < byteArray.length) {
            System.out.println("File reception incomplete!");
        }
        return byteArray;
    }

    public static void sendBytes(byte[] byteArray, PrintWriter stringOut,OutputStream byteOut, BufferedReader stringIn) throws IOException {
        stringOut.println(byteArray.length);
        stringOut.flush();
        stringIn.readLine(); // wait for ready signal from receiver
        byteOut.write(byteArray, 0, byteArray.length);
        byteOut.flush();
    }

}