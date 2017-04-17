package cp2;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.*;
import common.utils;

public class ClientCP2 {
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
                utils.closeConnections(byteOut, byteIn, stringOut, stringIn, socket);
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
                utils.closeConnections(byteOut, byteIn, stringOut, stringIn, socket);
            }

            System.out.println("Server verification successful.");
            System.out.println("Starting session");
            SecretKey sessionKey = generateAesKey();
            byte[] encodedKey = sessionKey.getEncoded();
            // create cipher object (encrypt), and initialize with public key
            Cipher rsaCipherEncrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipherEncrypt.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            byte[] encryptedSession = rsaCipherEncrypt.doFinal(encodedKey);
            utils.sendBytes(encryptedSession, stringOut, byteOut, stringIn);
            utils.getMessage(stringIn);
            
            utils.sendMessage(stringOut, "Ready to transmit file", IDENTITY);
            /* START OF FILE TRANSFER */
            System.out.println("Initializing File transfer...");
            utils.getMessage(stringIn);
            Cipher aesEncrypter = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aesEncrypter.init(Cipher.ENCRYPT_MODE, sessionKey);
            // compress the file 
            String tarName = utils.compressFile(args[0]);
            utils.sendMessage(stringOut, tarName, IDENTITY);

            // load file and encrypt to transmit
            byte[] encryptedFile = utils.loadAndEncryptFile(tarName, aesEncrypter);

            // send encrypted file
            utils.sendBytes(encryptedFile, stringOut, byteOut, stringIn);
            System.out.println("Done uploading file!");

            // receive confirmation message
            utils.getMessage(stringIn);
            utils.closeConnections(byteOut, byteIn, stringOut, stringIn, socket);
            System.out.println("Connections closed");
            utils.cleanUpFile(tarName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey key = keyGen.generateKey();
        return key;
    }
    
}
