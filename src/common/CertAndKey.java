package common;

import java.io.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;


public class CertAndKey {
  public static PrivateKey loadPrivateKey() throws Exception {
    Path privateKeyPath = Paths.get("assets/privateServer.der");
    byte[] privateKeyByteArray = Files.readAllBytes(privateKeyPath);

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyByteArray);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

    return privateKey;
  }

  public static byte[] loadSignedCertificate() throws Exception {
    File certFile = new File("assets/signedcert.crt");
    byte[] certByteArray = new byte[(int)certFile.length()];
    BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(certFile));
    bufferedInputStream.read(certByteArray, 0, certByteArray.length);
    return certByteArray;
  }
}