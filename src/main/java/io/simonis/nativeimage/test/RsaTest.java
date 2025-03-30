package io.simonis.nativeimage.test;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class RsaTest {
    public static void main(String[] args) throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("<RETURN>");
        in.readLine();
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        System.out.println("RSA KeyPairGenerator provider: " + rsaGen.getProvider().getName());
        rsaGen.initialize(1024);
        KeyPair rsa = rsaGen.generateKeyPair();
        PrivateKey rsaPrivate = rsa.getPrivate();
        PublicKey rsaPublic = rsa.getPublic();
        String message = args.length == 0 ? "RSA encryption/decryption test" : args[0];
        Cipher rsaEncryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        System.out.println("RSA Cipher provider: " + rsaEncryptCipher.getProvider().getName());
        rsaEncryptCipher.init(Cipher.ENCRYPT_MODE, rsaPublic);
        byte[] encrypted = rsaEncryptCipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        System.out.println("Encrypted message: " + Base64.getEncoder().encodeToString(encrypted));
        Cipher rsaDecryptCipher = Cipher.getInstance("RSA");
        rsaDecryptCipher.init(Cipher.DECRYPT_MODE, rsaPrivate);
        byte[] decrypted = rsaDecryptCipher.doFinal(encrypted);
        String decryptedMessage = new String(decrypted, StandardCharsets.UTF_8);
        assert message.equals(decryptedMessage);
        System.out.println("Decrypted message: " + decryptedMessage);
        System.out.print("<RETURN>");
        in.readLine();
    }
}
