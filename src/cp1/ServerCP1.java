package cp1;

import javax.crypto.Cipher;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import common.utils;
import common.CertAndKey;

public class ServerCP1 {
    private static final int NTHREADS = 5;
    private static ExecutorService executorService = new ScheduledThreadPoolExecutor(NTHREADS);

    private static ServerSocket serverSocket;
    private static final int PORT_NUMBER = 4321;

    private static final int IDENTITY = 0; // server


    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true) {
            try {
                System.out.println("... expecting connection ...");
                final Socket socket = serverSocket.accept();
                System.out.println("... connection established...");

                // create threads to handle multiple client uploads
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        try {
                          if (args.length > 0) handleClient(socket, args[0]);
                          else handleClient(socket, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                executorService.execute(task);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static void handleClient(Socket socket, String outputDat) throws Exception {
        // channels for sending and receiving bytes
        OutputStream byteOut = socket.getOutputStream();
        InputStream byteIn = socket.getInputStream();

        // channels for sending and receiving plain text
        PrintWriter stringOut = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader stringIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Let client initiate contact
        utils.getMessage(stringIn);

        // reply to client with server name
        utils.sendMessage(stringOut, "Hi, server name is SecStore!", IDENTITY);

        // get 64 bit nonce from client
        utils.getMessage(stringIn);
        byte[] nonce = utils.getBytes(stringIn, byteIn, stringOut);

        // load private key
        PrivateKey privateKey = CertAndKey.loadPrivateKey();

        // create cipher object (encrypt), initialize with private key
        Cipher rsaCipherEncrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipherEncrypt.init(Cipher.ENCRYPT_MODE, privateKey);

        // encrypt nonce and send to client
        byte[] encryptedNonce = rsaCipherEncrypt.doFinal(nonce);
        utils.sendBytes(encryptedNonce, stringOut, byteOut, stringIn);

        // Send signed CA certificate to client
        utils.getMessage(stringIn);
        byte[] certByteArray = CertAndKey.loadSignedCertificate();
        utils.sendBytes(certByteArray, stringOut, byteOut, stringIn);

        // create cipher object (decrypt), initialize with private key
        Cipher rsaCipherDecrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipherDecrypt.init(Cipher.DECRYPT_MODE, privateKey);
        utils.getMessage(stringIn);
        
        /* START OF FILE TRANSFER */
        utils.tic();
        // get encrypted file from client
        utils.sendMessage(stringOut, "Give me file name please!", IDENTITY);
        String fileName = "upload/" + utils.getMessage(stringIn).substring(9);
        byte[] encryptedFile = utils.getBytes(stringIn, byteIn, stringOut);
        // decrypt and save file
        utils.decryptAndSaveFile(encryptedFile, rsaCipherDecrypt, fileName);
        double timeTaken = utils.toc();
        // decompress file
        utils.decompressFile(fileName);
        utils.cleanUpFile(fileName);
        // inform client of successful file transfer
        utils.sendMessage(stringOut, "File Transfer success!", IDENTITY);
        
        if (outputDat != null) {
          FileWriter fw = new FileWriter(outputDat, true);
          BufferedWriter bw = new BufferedWriter(fw);
          String content = Double.toString(timeTaken);
          bw.write(content, 0, content.length());
          bw.flush();
          bw.newLine();
          bw.flush();
          bw.close();
          fw.close();
        }
    }

}
