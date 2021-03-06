package file_sender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Client {
    private final Socket SOCKET; // Socket used for connection
    private Streams streams; // The terminal used to send messages

    // TODO: Separate the server and client projects

    // Used to send commands to the server
    public void commandLine() {
        // Confirm that you have successfully connected to the server
        Logger.print("Successfully connected to " + SOCKET.getInetAddress()
                + ".\nYou should now be able to type in commands :)\n");

        // Loop until the client closes the connection
        boolean exit = false;

        // Loop until the client closes the connection
        while (!exit) {
            // Client should be able to send in commands
            Logger.print("> ");
            String command = Logger.scanLine().trim();
            Logger.log("Sent command to " + SOCKET.getInetAddress() + ":\n\"" + command + "\"");

            if (command.startsWith("exit")) {
                // If the client sends the "exit" command, close the connection
                streams.send(command);
                Logger.print("Closing connection.\n");
                exit = true;
                Logger.close();
                streams.closeStreams();
            } else if (command.startsWith("ls") || command.startsWith("dir")) {
                // If the client sends the "ls" or "dir" command, print out a list of files and
                // folders
                streams.send(command);
                int numberOfFiles = Integer.parseInt(streams.receive());
                for (int x = 0; x < numberOfFiles; x++)
                    Logger.print(streams.receive());

                // Do nothing if the user typed in nothing

            } else if (command.startsWith("cd")) {
                // If the client sends the "cd" command, change the directory
                streams.send(command);
                Logger.print(streams.receive() + "\n");
            } else if (command.startsWith("mkdir")) {
                // If the client sends the "mkdir" command, make a new directory
                streams.send(command);
                Logger.print(streams.receive() + "\n");
            } else if (command.startsWith("rm") || command.startsWith("del")) {
                // If the client sends the rm/del command, delete a file/directory

                // First send the command
                streams.send(command);

                // Now figure out how many lines we're supposed to receive
                int lines = Integer.parseInt(streams.receive());

                // Loop through each received line and print every single one out
                String toPrint = "";
                for (int i = 0; i < lines; i++)
                    toPrint += streams.receive();
                Logger.print(toPrint);
            } else if (command.startsWith("send")) {
                // If the client sends the "send" command, send a file to the server
                String filename = command.split(" ")[1];

                // Get the file and check whether it exists or not
                File f = new File(filename);
                if (f.exists()) {
                    try {
                        // Create the input streams and reader
                        FileInputStream fin = new FileInputStream(f);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));

                        // Count how many lines there are
                        int lines = 0;
                        while (reader.readLine() != null)
                            lines++;

                        // Send the "send [lines]" command to the server
                        streams.send("send " + lines + " " + f.getName());

                        // Reset the reader (this is necessary because we have already reached the end
                        // of the file)
                        fin.getChannel().position(0);
                        reader = new BufferedReader(new InputStreamReader(fin));

                        // Send each line of the file to the server
                        String line;
                        while ((line = reader.readLine()) != null)
                            streams.send(line);

                        // Close the reader and input stream
                        fin.close();
                        reader.close();

                        // Wait for the server to reply
                        Logger.print(streams.receive() + "\n");
                    } catch (IOException e) {
                        Logger.error("Error while trying to send file " + filename + ".");
                        Logger.error(e);
                    }
                } else {
                    // Tell the user that the file does not exist
                    Logger.print("File " + filename + " does not exist.");
                }
            } else if (command.startsWith("cat")) {
                // Send the command to the server
                streams.send(command);
                
                // Receive the amount of lines that will be read
                int lines = Integer.parseInt(streams.receive());
                
                // Read and print out every single line
                for(int i = 0; i < lines; i++)
                    Logger.print(streams.receive() + "\n");
            } else if (command.trim().equals("")) {
                // Do nothing
            } else if (command.startsWith("help")) {
                // If the client sends the "help" command, print out a list of commands
                String toPrint = "Here are a list of commands:\n" + "- ls: Used to list files in current directory.\n"
                        + "- dir: Same as ls.\n" + "- mkdir: Creates a new directory.\n" + "- cd: Change directory.\n"
                        + "- send: sends a file to the server.\n" + "- cat: Displays the contents of a file.\n"
                        + "- del: Deletes a file or directory.\n" + "- rm: Same as del.\n"
                        + "- exit: Close the connection.\n";
                Logger.print(toPrint);
            } else {
                // Else state that the client did not use a usable command
                Logger.print("\"" + command + "\" is not a usable command.\n");
            }
        }
    }

    // Used to print out received data
    // This should be ran on a separate thread
    public void printReceived() {

    }

    // Constructor
    public Client() throws IOException, NoSuchAlgorithmException {
        // Ask for information about the server
        String ip = Logger.ask("Enter the server's IP address: ");
        int port = Integer.parseInt(Logger.ask("Enter the port number: "));

        // Setup socket and terminal connections
        Logger.print("Setting up connection with server...\n");
        SOCKET = new Socket(ip, port);
        streams = new Streams(SOCKET);

        // TODO: Ask for information via command line arguments

        // Generate an RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair kp = keyGen.generateKeyPair();

        // Send out our public key to the server
        byte[] publicKey = kp.getPublic().getEncoded();
        streams.sendUnencrypted(Base64.getEncoder().encodeToString(publicKey));

        try {
            // Recieve and decrypt the AES key sent from the server
            byte[] encryptedKey = Base64.getDecoder().decode(streams.receiveUnencrypted());
            Cipher c = Cipher.getInstance("RSA");
            c.init(Cipher.DECRYPT_MODE, kp.getPrivate());
            byte[] AESKey = c.doFinal(encryptedKey);

            // Make sure that the key is not null
            if (AESKey != null) {
                // Setup our received AES key
                streams.setKey(new SecretKeySpec(AESKey, 0, AESKey.length, "AES"));

                // Send the answer to the server's challenge
                // The server should send a random string as a challenge
                // The answer to the challenge is SHA256(SHA256(random string) +
                // SHA256(password))
                String password = Logger.ask("Enter the password: ");
                String challenge = streams.receive();
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String challengeHash = Base64.getEncoder()
                        .encodeToString(digest.digest(challenge.getBytes(StandardCharsets.UTF_8)));
                String passwordHash = Base64.getEncoder()
                        .encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
                String answer = Base64.getEncoder()
                        .encodeToString(digest.digest((challengeHash + passwordHash).getBytes(StandardCharsets.UTF_8)));
                streams.send(answer);

                // If the server did not state whether the answer is correct or not, close the
                // connection
                String challengeResponse = streams.receive();
                Logger.print(challengeResponse + "\n");
                if (!challengeResponse.equals("Correct password.")) {
                    streams.closeStreams();
                    Logger.close();
                    System.exit(0);
                }
            } else {
                // Close the connection if the server did not submit a proper key
                Logger.error("The server did not submit a proper key. Connection will be closed.");
                streams.closeStreams();
                Logger.close();
                System.exit(1);
            }
        } catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            Logger.error("Error while doing key exchange and password challenge.");
            Logger.error(e);
        }
    }

}
