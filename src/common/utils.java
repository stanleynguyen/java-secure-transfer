package common;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import javax.crypto.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        stringOut.println("ready"); // sinalling sender I'm ready to get bytes
        stringOut.flush();
        while (offset < byteArray.length && (numRead = byteIn.read(byteArray, offset, byteArray.length - offset)) >= 0) {
            offset += numRead;
            System.out.println(offset);
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
    
    public static byte[] loadAndEncryptFile(String fileName, Cipher encrypter) throws Exception {
        Path filePath = Paths.get(fileName);
        byte[] fileByteArray = Files.readAllBytes(filePath);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int pointer = 0;
        int fileByteArrayLength = fileByteArray.length;
        while (pointer < fileByteArrayLength) {
            byte[] placeHolder;
            if (fileByteArrayLength - pointer >= 117) {
                placeHolder = encrypter.doFinal(fileByteArray, pointer, 117);
            } else {
                placeHolder = encrypter.doFinal(fileByteArray, pointer, fileByteArrayLength - pointer);
            }
            byteArrayOutputStream.write(placeHolder, 0, placeHolder.length);
            pointer += 117;
        }
        byte[] encryptedFile = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        return encryptedFile;
    }
    
    public static void decryptAndSaveFile(byte[] encryptedFile, Cipher decrypter, String fileName)
            throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int count = 0;
        int encryptedFileLength = encryptedFile.length;
        while (count < encryptedFileLength) {
            byte[] placeHolder;
            if (encryptedFileLength - count >= 128) {
                placeHolder = decrypter.doFinal(encryptedFile, count, 128);
            } else {
                placeHolder = decrypter.doFinal(encryptedFile, count, encryptedFileLength - count);
            }
            byteArrayOutputStream.write(placeHolder, 0, placeHolder.length);
            count += 128;
        }
        byte[] decryptedFile = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        // create dir if it does not exist
        File file = new File(fileName);
        file.getParentFile().mkdirs();

        // create new file and write to file
        FileOutputStream fileOut = new FileOutputStream(fileName);
        fileOut.write(decryptedFile, 0, decryptedFile.length);
        System.out.println("File saved into server.");
    }
    
    public static void closeConnections(OutputStream byteOut, InputStream byteIn, PrintWriter stringOut,
                                         BufferedReader stringIn, Socket socket) throws Exception {
        byteOut.close();
        byteIn.close();
        stringOut.close();
        stringIn.close();
        socket.close();
    }
    
    public static String compressFile(String fileStr) throws IOException {
        Pattern p = Pattern.compile("[^\\/\\\\]+$");
        Matcher m = p.matcher(fileStr);
        String fileName = null;
        if (!m.find()) {
            System.out.println("Could not parse file name from path");
        }
        fileName = m.group();
        String dirName = fileStr.substring(0, fileStr.length() - fileName.length());
        System.out.println(dirName);
        if (dirName.length() == 0) dirName = ".";
        String tarName = fileName + ".tar";
        ProcessBuilder pb = new ProcessBuilder("tar", "-C", dirName, "-czf", tarName, fileName);
        pb.start();
        return tarName;
    }
    
    public static void decompressFile(String fileName) throws IOException {
      Pattern p = Pattern.compile("[^\\/\\\\]+$");
      Matcher m = p.matcher(fileName);
      String tarName = null;
      if (!m.find()) {
          System.out.println("Could not parse file name from path");
      }
      tarName = m.group();
      System.out.println(tarName);
      ProcessBuilder pb = new ProcessBuilder("tar", "-xzf", tarName, "-C", "upload");
      pb.start();
    }
    
    public static void cleanUpFile(String fileName) throws IOException {
      ProcessBuilder pb = new ProcessBuilder("rm", fileName);
      pb.start();
    }
}
