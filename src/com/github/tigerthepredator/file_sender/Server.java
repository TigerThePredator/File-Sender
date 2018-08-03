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
    private final File FOLDER; // Top-level directory that everyone is supposed to be able to access
    private String passwordHash; // Password to enter the server. Saved as a SHA256 hash

    // TODO: Add different groups and users, and allow the server admins to give
    // different permissions
    // TODO: Allow multiple clients to connect to the server at once
    // TODO: Add support for vi/vim
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
                        Logger.log(
                                "Command received from " + clientSocket.getInetAddress() + ":\n\"" + inputLine + "\"");

                        if (inputLine.startsWith("ls") || inputLine.startsWith("dir")) {
                            // List the files if client sends the "ls" command

                            // Get the list of folders
                            String[] list = currentFolder.list();

                            if (list != null) {
                                // Send the length of the list
                                streams.send(list.length + "");

                                // Send each file/folder in the list
                                for (String file : list)
                                    streams.send(file + "\n");
                            }

                            // TODO: Indicate which items are files and which items are folders
                            // TODO: Be able to display the output of ls -la by default

                        } else if (inputLine.startsWith("cd")) {
                            // Change directory if client sends the "cd" command

                            // TODO: Allow access to folders with spaces in their names
                            String[] split = inputLine.split(" ");
                            if (split.length > 1) {
                                if (split[1].equals("..")) {
                                    // If the user types in "..", go to the parent directory
                                    File newFolder = currentFolder.getParentFile();
                                    
                                    // Check for directory traversal attacks
                                    if (isSubDirectory(FOLDER, newFolder)) {
                                        // Switch the folder
                                        currentFolder = newFolder;
                                        streams.send("Changed to parent directory.");
                                    } else
                                        streams.send("The folder that you requested does not exist.");
                                } else {
                                    // Else if the user types in any other subdirectory, go to that subdirectory
                                    File newFolder = new File(currentFolder.getAbsolutePath() + "/" + split[1]);
                                    // Check if the directory exists
                                    if ((newFolder.exists()) && (newFolder.isDirectory())
                                    // This last conditional is checked to prevent directory traversal attacks
                                            && isSubDirectory(FOLDER, newFolder)) {
                                        currentFolder = newFolder;
                                        streams.send("Changed to /" + split[1] + " directory.");
                                    } else
                                        streams.send("The folder that you requested does not exist.");
                                }
                            } else {
                                streams.send("You must enter in a folder name.");
                            }
                        } else if (inputLine.startsWith("mkdir")) {
                            // Make a new directory if the user sends this command
                            // TODO: Allow the creation of folders with spaces in their names

                            // Split the line to get the folder name
                            String[] split = inputLine.split(" ");
                            if (split.length > 1) {
                                // Create the new folder
                                File newFolder = new File(currentFolder.getAbsolutePath() + "/" + split[1]);
                                if (newFolder.mkdirs()) {
                                    streams.send("Successfully created " + split[1] + " directory.");
                                    Logger.log("System change:\n" + newFolder.getAbsolutePath()
                                            + " directory has been created.");
                                } else {
                                    streams.send("Error occurred while generating directory.");
                                }
                            } else {
                                streams.send("Directory must have a name.");
                            }
                        } else if (inputLine.startsWith("rm") || inputLine.startsWith("del")) {
                            // Delete a file/folder

                            // Get a list of all the files that will be deleted
                            String files[] = inputLine.split(" ");

                            // Everything that will be sent to the client
                            String toSend = "";

                            // Loop through each file and delete each one
                            // i starts at 1 because the zeroith input is the actual "rm" or "del" command
                            for (int i = 1; i < files.length; i++) {
                                // Get the file
                                File toDel = new File(currentFolder.getAbsolutePath() + "/" + files[i]);

                                // Delete the file if it exists
                                if (toDel.exists())
                                    if (toDel.delete()) {
                                        toSend += "System has deleted " + toDel.getName() + ".\n";
                                        Logger.log("System change:\n" + toDel.getAbsolutePath() + " has been deleted.");
                                    } else
                                        toSend += toDel.getName() + " is not a file or directory.\n";
                            }

                            // First send how many lines will be sent
                            // TODO: Figure out why the user cannot delete multiple items at once
                            streams.send(toSend.length() - toSend.replace("\n", "").length() + "");

                            // Now send everything
                            streams.send(toSend);
                        } else if (inputLine.startsWith("exit")) {
                            // Close the connection if client sends the "exit" command

                            Logger.print("Client at " + clientSocket.getInetAddress().getHostAddress()
                                    + " has disconnected.\n");
                            streams.closeStreams();
                            clientSocket.close();
                            break;

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

    // Checks if a file/folder is a subdirectory of another folder
    // Copied from
    // http://www.java2s.com/Tutorial/Java/0180__File/Checkswhetherthechilddirectoryisasubdirectoryofthebasedirectory.htm
    public boolean isSubDirectory(File base, File child) throws IOException {
        base = base.getCanonicalFile();
        child = child.getCanonicalFile();

        File parentFile = child;
        while (parentFile != null) {
            if (base.equals(parentFile)) {
                return true;
            }
            parentFile = parentFile.getParentFile();
        }
        return false;
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
