package com.github.tigerthepredator.file_sender;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class Server {
    private final int PORT; // Port that the server resides on
    private final File FOLDER; // Folder that everyone is supposed to be able to access
    private String passwordHash; // Password to enter the server. Saved as a SHA256 hash

    // TODO: Store password as a hash
    // TODO: Add different groups and users, and allow the server admins to give
    // different permissions
    // TODO: Allow multiple clients to connect to the server at once
    // TODO: Add support for vi/vim
    // TODO: Add mkdir command
    // TODO: Add cat command
    // TODO: Add dl command
    // TODO: Add rm command

    // Starts a connection with the client
    public void connection() throws IOException, NoSuchAlgorithmException {
        // Initialize the server socket
        ServerSocket serverSocket = new ServerSocket(PORT);

        // TODO: Find a proper way to exit this while loop
        // TODO: Close the server socket connection once the while loop has exited
        while (true) {
            // Create the client socket and terminal connection
            Logger.print("Waiting for connection...\n");
            Socket clientSocket = serverSocket.accept();
            Streams streams = new Streams(clientSocket);
            Logger.print("Setting up communications with " + clientSocket.getInetAddress() + ".\n");

            // Conduct a key exchange
            if (streams.keyExchange()) {

                // The server will send a random string with length 10 to the client,
                // and the client should respond with SHA256(SHA256(random string) +
                // SHA256(password)) in order to start the connection. This way, neither the
                // server nor the client will ever transmit the real password

                // Develop a random 10-character string and send it as a challenge
                String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                StringBuilder challengeBuilder = new StringBuilder();
                Random r = new Random();
                for (int x = 0; x < 10; x++)
                    challengeBuilder.append(characters.charAt(Math.abs(r.nextInt() % characters.length())));
                String challenge = challengeBuilder.toString();
                streams.send(challenge);
                Logger.log(challenge + " sent as a challenge to " + clientSocket.getInetAddress() + ".");

                // Develop the correct answer to the challenge
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                String challengeHash = Base64.getEncoder()
                        .encodeToString(digest.digest(challenge.getBytes(StandardCharsets.UTF_8)));
                String answer = Base64.getEncoder()
                        .encodeToString(digest.digest((challengeHash + passwordHash).getBytes(StandardCharsets.UTF_8)));

                // Check if the client sent the correct answer
                if (answer.equals(streams.receive())) {
                    // Tell the client that they have successfully connected
                    streams.send("Correct password.");

                    // Print this on the server
                    Logger.print("Client connected from " + clientSocket.getInetAddress() + ".\n");

                    // Process the client's commands as he sends them
                    File currentFolder = FOLDER;
                    String inputLine;
                    while ((inputLine = streams.receive()) != null) {
                        // Print out what the client said
                        Logger.print(clientSocket.getInetAddress() + "> " + inputLine + "\n");
                        Logger.log(inputLine + " command sent from " + clientSocket.getInetAddress() + ".");

                        if (inputLine.startsWith("ls") || inputLine.startsWith("dir")) {
                            // List the files if client sends the "ls" command

                            // Get the list of folders
                            String[] list = currentFolder.list();

                            // Send the length of the list
                            streams.send(list.length + "");

                            // Send each file/folder in the list
                            for (String file : list)
                                streams.send(file + "\n");

                            // TODO: Indicate which items are files and which items are folders
                            // TODO: Be able to display the output of ls -la by default

                        } else if (inputLine.startsWith("cd")) {
                            // Change directory if client sends the "cd" command

                            // TODO: Prevent the client from accessing folders he is not supposed to access
                            // TODO: Allow access to folders with spaces in their names
                            String[] split = inputLine.split(" ");
                            if (split.length > 1) {
                                File newFolder = new File(currentFolder.getAbsolutePath() + "/" + split[1]);
                                if ((newFolder.exists()) && (newFolder.isDirectory())) {
                                    currentFolder = newFolder;
                                    streams.send("Changed to /" + split[1] + " directory.");
                                } else
                                    streams.send("The folder that you requested does not exist.");
                            } else {
                                streams.send("You must enter in a folder name.");
                            }
                        } else if (inputLine.startsWith("exit")) {
                            // Close the connection if client sends the "exit" command

                            Logger.print("Client at " + clientSocket.getInetAddress().getHostAddress()
                                    + " has disconnected.\n");
                            streams.closeStreams();
                            clientSocket.close();
                            break;

                        } else if (inputLine.startsWith("dl")) {
                            // Send files for the client to download if the client sends the "dl" command
                            // TODO: Finish this
                        }
                    }
                } else {
                    // Close the connection if the response to the challenge was incorrect
                    Logger.print(
                            clientSocket.getInetAddress() + " has sent an incorrect password. Closing connection.");
                    streams.send("You have sent an incorrect password. Closing connection.");
                    streams.closeStreams();
                    clientSocket.close();
                }
            } else {
                // End the connection if the client fails the key exchange
                Logger.print("Client at " + clientSocket.getInetAddress()
                        + " has failed the key exchange. Closing connection.\n");
                streams.closeStreams();
                clientSocket.close();
            }
        }

    }

    // Constructor
    public Server() {
        // TODO: Ask for information via command line arguments

        // Ask for important server information
        PORT = Integer.parseInt(Logger.ask("Enter the port number: "));
        FOLDER = new File(Logger.ask("Enter the folder that you would like to share: "));

        // Setup the password
        try {
            String password = Logger.ask("Enter in a password: ");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            passwordHash = Base64.getEncoder().encodeToString(md.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            Logger.error("Error while hashing the password.");
            Logger.error(e);
        }
    }
}
