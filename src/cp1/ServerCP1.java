package cp1;

import javax.crypto.Cipher;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by vivek on 9/4/17.
 */
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
                            handleClient(socket);
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

    private static void handleClient(Socket socket) throws Exception {
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
        byte[] nonce = utils.getBytes(stringIn, byteIn);

        // load private key
        PrivateKey privateKey = loadPrivateKey();

        // create cipher object (encrypt), initialize with private key
        Cipher rsaCipherEncrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipherEncrypt.init(Cipher.ENCRYPT_MODE, privateKey);

        // encrypt nonce and send to client
        byte[] encryptedNonce = rsaCipherEncrypt.doFinal(nonce);
        utils.sendBytes(encryptedNonce, stringOut, byteOut);

        // Send signed CA certificate to client
        utils.getMessage(stringIn);
        byte[] certByteArray = loadSignedCertificate();
        utils.sendBytes(certByteArray, stringOut, byteOut);

        // get encrypted file from client
        utils.getMessage(stringIn);
        byte[] encryptedFile = utils.getBytes(stringIn, byteIn);
        System.out.println(Arrays.toString(encryptedFile));

        // create cipher object (decrypt), initialize with private key
        Cipher rsaCipherDecrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipherDecrypt.init(Cipher.DECRYPT_MODE, privateKey);

        // decrypt and save file
        decryptAndSaveFile(encryptedFile, rsaCipherDecrypt, "src/output2.txt");
    }

    private static PrivateKey loadPrivateKey() throws Exception {
        Path privateKeyPath = Paths.get("src/privateServer.der");
        byte[] privateKeyByteArray = Files.readAllBytes(privateKeyPath);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyByteArray);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        return privateKey;
    }

    private static byte[] loadSignedCertificate() throws Exception {
        File certFile = new File("src/signedcert.crt");
        byte[] certByteArray = new byte[(int)certFile.length()];
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(certFile));
        bufferedInputStream.read(certByteArray, 0, certByteArray.length);
        return certByteArray;
    }

    private static void decryptAndSaveFile(byte[] encryptedFile, Cipher rsaCipherDecrypt, String fileName) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int count = 0;
        int encryptedFileLength = encryptedFile.length;
        while (count < encryptedFileLength) {
            byte[] placeHolder;
            if (encryptedFileLength - count >= 128) {
                placeHolder = rsaCipherDecrypt.doFinal(encryptedFile, count, 128);
            } else {
                placeHolder = rsaCipherDecrypt.doFinal(encryptedFile, count, encryptedFileLength - count);
            }
            byteArrayOutputStream.write(placeHolder, 0, placeHolder.length);
            count += 128;
        }
        byte[] decryptedFile = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        // create new file and write to file
        FileOutputStream fileOut = new FileOutputStream(fileName);
        fileOut.write(decryptedFile, 0, decryptedFile.length);
        System.out.println("File registered into system.");
    }

}
