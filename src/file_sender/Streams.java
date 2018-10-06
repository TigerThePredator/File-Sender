package file_sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class Streams {
    private Key key; // Key used for encryption
    private PrintWriter out; // Used to send data to server
    private BufferedReader in; // Used to read data from server

    // Constructor
    public Streams(Socket socket) {
        try {
            // Create I/O streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            out = null;
            in = null;
            Logger.error("Error while initializing streams");
            Logger.error(e);
        }
    }

    // Returns true if key exchange is successful
    public boolean keyExchange() {
        // Only the server should call the keyExchange() function
        // The client should never call this function
        // AES will be used as the key, but RSA is used for the key exchange
        // Step 1: Client generates RSA public/private key pair
        // Step 2: Client sends unencrypted RSA public key
        // Step 3: Server encrypts AES key with client's RSA public key
        // Step 4: Server sends encrypted AES key to client
        // Step 5: Both parties now have the correct AES key and communications can
        // begin

        try {
            // Receive the client's public RSA key
            byte[] encodedClientKey = Base64.getDecoder().decode(receiveUnencrypted());
            Key clientRSAKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encodedClientKey));

            // Generate AES key
            KeyGenerator aesKeyGen = KeyGenerator.getInstance("AES");
            aesKeyGen.init(256);
            key = aesKeyGen.generateKey();

            // Encrypt the AES key using the client's RSA public key
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.ENCRYPT_MODE, clientRSAKey);
            byte[] encryptedAESKey = c.doFinal(key.getEncoded());

            // We will be using the sendUnencrypted() function because we already encrypted
            // the key with the client's RSA public key and because send() only uses AES to
            // encrypt data
            sendUnencrypted(Base64.getEncoder().encodeToString(encryptedAESKey));
            return true; // Return true if successful
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException | InvalidKeySpecException e) {
            Logger.error("Error during key exchange.");
            Logger.error(e);
            return false;
        }
    }

    // Sets the AES key being used
    public void setKey(Key key) {
        // Only the client should call this method after being sent the AES key from the
        // server
        this.key = key;
    }

    // Send an encrypted message
    public void send(String message) {
        try {
            // Encrypt the message
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
            String encoded = Base64.getEncoder().encodeToString(message.getBytes("utf-8"));
            byte[] encrypted = c.doFinal(encoded.getBytes());
            String encryptedString = Base64.getEncoder().encodeToString(encrypted);

            // Send the encrypted message
            out.println(encryptedString);
            out.flush();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException | UnsupportedEncodingException e) {
            Logger.error("Error while sending encrypted message.");
            Logger.error(e);
        }
    }

    // Send an unencrypted message
    public void sendUnencrypted(String message) {
        out.println(message);
        out.flush();
    }

    // Receive an encrypted message
    public String receive() {
        try {
            // Wait until the stream is ready to be read
            while (true)
                if (in.ready())
                    break;

            // Obtain the encrypted message
            String encrypted = in.readLine();

            // Decrypt and return the message
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            String utf8 = new String(c.doFinal(decoded));
            String plaintext = new String(Base64.getDecoder().decode(utf8));

            // Return the message
            return plaintext;
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            Logger.error("Error while receiving encrypted message.");
            Logger.error(e);
        }
        return null;
    }

    // Receive an unencrypted message
    public String receiveUnencrypted() {
        try {
            // Wait until the stream is ready to be read
            while (true)
                if (in.ready())
                    break;

            return in.readLine();
        } catch (IOException e) {
            Logger.error("Error while receiving unencrypted message.");
            Logger.error(e);
        }
        return null;
    }

    // Close the streams
    public void closeStreams() {
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            Logger.error("Error while closing streams");
            Logger.error(e);
        }
    }
}
