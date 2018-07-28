package com.github.tigerthepredator.file_sender;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Server {
	private final int PORT; // Port that the server resides on
	private final String PASSWORD; // Password to enter the server
	private final File FOLDER; // Folder that everyone is supposed to be able to access
	private final KeyPair KEYS; // The server's public/private key pair

	// TODO: Store password as a hash
	// TODO: Add different groups and users, and allow the server admins to give
	// different permissions
	// TODO: Allow multiple clients to connect to the server at once

	// Starts a connection with the client
	public void connection() throws IOException, NoSuchAlgorithmException {
		// Initialize the server socket
		ServerSocket serverSocket = new ServerSocket(PORT);

		// TODO: Find a proper way to exit this while loop
		// TODO: Close the server socket connection once the while loop has exited
		while (true) {
			// Create the client socket and terminal connection
			Terminal.print("Waiting for connection...\n");
			Socket clientSocket = serverSocket.accept();
			Terminal terminal = new Terminal(clientSocket);
			Terminal.confirm("Client connected from " + clientSocket.getInetAddress() + ".");

			// Receive the client's public key
			Key clientKey = Encryptor.stringToKey(terminal.receiveUnencrypted());
			if (clientKey != null) { // Make sure that the key is not null
				// Send the client the server's public key
				terminal.send(Encryptor.keyToString(KEYS.getPublic()), clientKey);

				// Develop a challenge for the client
				// The server will send a random string with length 10 to the client,
				// and the client should respond with SHA256(random string + password) in order
				// to start the connection
				String challenge = Encryptor.randString(10); // Develop random string
				terminal.send(challenge, clientKey); // Send the challenge string
				challenge = challenge + PASSWORD; // Append the password to the challenge string
				MessageDigest digest = MessageDigest.getInstance("SHA-256"); // Create a SHA256 message digest
				// Set encodedhash = SHA256(random string + password)
				String encodedhash = Encryptor.bytesToHex(digest.digest(challenge.getBytes(StandardCharsets.UTF_8)));
				if (terminal.receive(KEYS.getPrivate()) != encodedhash) { // Check if the client sent the correct
																			// response
					// Close the connection if the client fails the challenge
					terminal.send("You have sent an incorrect password. Closing connection", clientKey);
				} else { // Allow access to the folder if the client passed the challenge
					// Process the client's commands as he sends them
					File currentFolder = FOLDER;
					String inputLine;
					while ((inputLine = terminal.receive(KEYS.getPrivate())) != null) {
						// List the files if client sends the "ls" command
						if (inputLine.startsWith("ls")) {
							for (String file : currentFolder.list())
								terminal.send(file + "\n", clientKey);

							// Change directory if client sends the "cd" command
						} else if (inputLine.startsWith("cd")) {
							// TODO: Prevent the client from accessing folders he is not supposed to access
							File newFolder = new File(currentFolder.getAbsolutePath() + inputLine.split(" ")[1]);
							if ((newFolder.exists()) && (newFolder.isDirectory())) {
								currentFolder = newFolder;
							} else
								terminal.send("The folder that you requested does not exist.", clientKey);

							// Close the connection if client sends the "exit" command
						} else if (inputLine.equals("exit")) {
							Terminal.print("Client at " + clientSocket.getInetAddress().getHostAddress()
									+ " has disconnected.\n");
							terminal.closeStreams();
							clientSocket.close();
							break;

							// Send files for the client to download if the client sends the "dl" command
						} else if (inputLine.startsWith("dl")) {
							// TODO: Finish this

							// Send a list of commands if the client sends the "help" command
						} else if (inputLine.startsWith("help")) {
							// TODO: List commands

							// If the client sends an incorrect command, let them know
						} else {
							terminal.send("\'" + inputLine + "\' is not a usable command.", clientKey);
						}
					}
				}
			}
		}

	}

	// Constructor
	public Server() {
		// Generate a key pair
		KEYS = Encryptor.generateKeyPair();
		if (KEYS == null) {
			Terminal.error("Fatal error while generating keys. Exiting program...");
			System.exit(1);
		}

		// TODO: Ask for information via command line arguments

		// Ask for important information
		PORT = Integer.parseInt(Terminal.ask("Enter the port number: "));
		FOLDER = new File(Terminal.ask("Enter the folder that you would like to share: "));
		PASSWORD = Terminal.ask("Enter in a password: ");
	}
}
