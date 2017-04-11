package cp1;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Created by vivek on 9/4/17.
 */
public class ClientCP1 {
    private static final String SERVER_NAME = "localhost";
    private static final int SERVER_PORT = 4321;
    private static final int IDENTITY = 1; // client

    public static void main(String[] args) {
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
                System.out.println("wrong server, closing connections");
                // close connections
            }

            // send nonce
            byte[] nonce = utils.generateNonce();
            utils.sendMessage(stringOut, "Sending a 64 bit nonce, please verify your identity!", IDENTITY);
            utils.sendBytes(nonce, stringOut, byteOut);

            // receive encrypted nonce
            byte[] encryptedNonce = utils.getBytes(stringIn, byteIn);

            // ask for signed CA certificate
            utils.sendMessage(stringOut, "Give me your signed CA certificate!", IDENTITY);
            byte[] certByteArray = utils.getBytes(stringIn, byteIn);

            // load public key from CA certificate
            InputStream inputStream = new FileInputStream("src/CA.crt");
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
                // close connections
            }

            System.out.println("Server verification successful.");
            utils.sendMessage(stringOut, "Ready to transmit file", IDENTITY);

            /* START OF FILE TRANSFER */
            System.out.println("Initializing File transfer...");

            // create cipher object (encrypt), and initialize with public key
            Cipher rsaCipherEncrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipherEncrypt.init(Cipher.ENCRYPT_MODE, serverPublicKey);

            // load file and encrypt to transmit
            byte[] encryptedFile = loadAndEncryptFile("src/smallFile.txt", rsaCipherEncrypt);
            System.out.println(Arrays.toString(encryptedFile));
            // send encrypted file
            utils.sendBytes(encryptedFile, stringOut, byteOut);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] loadTransmitFile() throws Exception {
        File transmitFile = new File("src/medianFile.txt");
        byte[] fileByteArray = new byte[(int)transmitFile.length()];
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(transmitFile));
        bufferedInputStream.read(fileByteArray, 0, fileByteArray.length);
        return fileByteArray;
    }

    private static byte[] loadAndEncryptFile(String fileName, Cipher rsaCipherEncrypt) throws Exception {
        File transmitFile = new File(fileName);
        byte[] fileByteArray = new byte[(int)transmitFile.length()];
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(transmitFile));
        bufferedInputStream.read(fileByteArray, 0, fileByteArray.length);

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
}
