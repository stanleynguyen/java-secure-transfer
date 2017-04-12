package cp1;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.Files.*;

/**
 * Created by vivek on 9/4/17.
 */
public class ClientCP1 {
    private static final String SERVER_NAME = "localhost";
    private static final int SERVER_PORT = 4321;
    private static final int IDENTITY = 1; // client

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Please enter file to be uploaded.");
            System.exit(1);
        }

        Path uploadFile = Paths.get(args[0]);

        try {
            // create TCP socket to communicate with server
            Socket socket = new Socket(SERVER_NAME, SERVER_PORT);

            // channels for sending and receiving bytes
            OutputStream byteOut = socket.getOutputStream();
            InputStream byteIn = socket.getInputStream();

            // channels for sending and receiving string
            PrintWriter stringOut = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader stringIn =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

            // ask for server name and check name
            utils.sendMessage(stringOut, "Hello SecStore, please prove your identity!", IDENTITY);
            String serverName = utils.getMessage(stringIn);
            if (!serverName.contains("SecStore")) {
                System.out.println("Wrong server. Closing all connections...");
                closeConnections(byteOut, byteIn, stringOut, stringIn, socket);
            }

            // send nonce
            byte[] nonce = utils.generateNonce();
            utils.sendMessage(stringOut, "Sending a 64 bit nonce, please verify your identity!", IDENTITY);
            utils.sendBytes(nonce, stringOut, byteOut, stringIn);

            // receive encrypted nonce
            byte[] encryptedNonce = utils.getBytes(stringIn, byteIn, stringOut);

            // ask for signed CA certificate
            utils.sendMessage(stringOut, "Give me your signed CA certificate!", IDENTITY);
            byte[] certByteArray = utils.getBytes(stringIn, byteIn, stringOut);

            // load public key from CA certificate
            InputStream inputStream = new FileInputStream("assets/CA.crt");
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(inputStream);
            PublicKey caPublicKey = caCert.getPublicKey();

            // verify signed CA certificate with public key from certificate
            InputStream certInputStream = new ByteArrayInputStream(certByteArray);
            X509Certificate signedCertificate = (X509Certificate) certificateFactory.generateCertificate(certInputStream);

            signedCertificate.checkValidity();
            signedCertificate.verify(caPublicKey);
            System.out.println("Signed CA certificate valid and verified");

            // extract public key from signed CA certificate
            PublicKey serverPublicKey = signedCertificate.getPublicKey();

            // create cipher object (decrypt), and initialize with public key
            Cipher rsaCipherDecrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipherDecrypt.init(Cipher.DECRYPT_MODE, serverPublicKey);

            // decrypt nonce
            byte[] decryptedNonce = rsaCipherDecrypt.doFinal(encryptedNonce);

            // verify nonce
            if (!Arrays.equals(nonce, decryptedNonce)) {
                System.out.println("Server verification failed. Closing all connections...");
                closeConnections(byteOut, byteIn, stringOut, stringIn, socket);
            }

            System.out.println("Server verification successful.");
            utils.sendMessage(stringOut, "Ready to transmit file", IDENTITY);

            // create cipher object (encrypt), and initialize with public key
            Cipher rsaCipherEncrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipherEncrypt.init(Cipher.ENCRYPT_MODE, serverPublicKey);

            /* START OF FILE TRANSFER */
            System.out.println("Initializing File transfer...");
            utils.getMessage(stringIn);

            // get file name from path and send to server
            Pattern p = Pattern.compile("[^\\/\\\\]+$");
            Matcher m = p.matcher(args[0]);
            System.out.println(args[0]);
            String fileName = null;
            if (!m.find()) {
                System.out.println("Could not parse file name from path");
            }
            fileName = m.group();
            System.out.println(fileName);
            utils.sendMessage(stringOut, fileName, IDENTITY);

            // load file and encrypt to transmit
            byte[] encryptedFile = loadAndEncryptFile(args[0], rsaCipherEncrypt);

            // send encrypted file
            utils.sendBytes(encryptedFile, stringOut, byteOut, stringIn);
            System.out.println("Done uploading file!");

            // receive confirmation message
            utils.getMessage(stringIn);
            closeConnections(byteOut, byteIn, stringOut, stringIn, socket);
            System.out.println("Connections closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] loadAndEncryptFile(String fileName, Cipher rsaCipherEncrypt) throws Exception {
        Path filePath = Paths.get(fileName);
        byte[] fileByteArray = readAllBytes(filePath);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        int pointer = 0;
        int fileByteArrayLength = fileByteArray.length;
        while (pointer < fileByteArrayLength) {
            byte[] placeHolder;
            if (fileByteArrayLength - pointer >= 117) {
                placeHolder = rsaCipherEncrypt.doFinal(fileByteArray, pointer, 117);
            } else {
                placeHolder = rsaCipherEncrypt.doFinal(fileByteArray, pointer, fileByteArrayLength - pointer);
            }
            byteArrayOutputStream.write(placeHolder, 0, placeHolder.length);
            pointer += 117;
        }
        byte[] encryptedFile = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        return encryptedFile;
    }

    private static void closeConnections(OutputStream byteOut, InputStream byteIn, PrintWriter stringOut,
                                         BufferedReader stringIn, Socket socket) throws Exception {
        byteOut.close();
        byteIn.close();
        stringOut.close();
        stringIn.close();
        socket.close();
    }
}
