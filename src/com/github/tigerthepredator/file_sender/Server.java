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
	// TODO: Add server logs

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
			Streams streams = new Streams(clientSocket);
			Terminal.confirm("Setting up communications with " + clientSocket.getInetAddress() + ".");

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
					Terminal.confirm("Client connected from " + clientSocket.getInetAddress() + ".");
					
					// Process the client's commands as he sends them
					File currentFolder = FOLDER;
					String inputLine;
					while ((inputLine = streams.receive()) != null) {
						// Print out what the client said
						Terminal.print(clientSocket.getInetAddress() + "> " + inputLine + "\n");
						
						// List the files if client sends the "ls" command
						if (inputLine.startsWith("ls")) {
							// Get the list of folders
							String[] list = currentFolder.list();
							
							// Send the length of the list
							streams.send(list.length + "");
							
							// Send each file/folder in the list
							for (String file : list)
								streams.send(file + "\n");
							
							// TODO: Indicate which items are files and which items are folders

							// Change directory if client sends the "cd" command
						} else if (inputLine.startsWith("cd")) {
							// TODO: Prevent the client from accessing folders he is not supposed to access
							File newFolder = new File(currentFolder.getAbsolutePath() + inputLine.split(" ")[1]);
							if ((newFolder.exists()) && (newFolder.isDirectory())) {
								currentFolder = newFolder;
							} else
								streams.send("The folder that you requested does not exist.");

							// Close the connection if client sends the "exit" command
						} else if (inputLine.startsWith("exit")) {
							Terminal.print("Client at " + clientSocket.getInetAddress().getHostAddress()
									+ " has disconnected.\n");
							streams.closeStreams();
							clientSocket.close();
							break;

							// Send files for the client to download if the client sends the "dl" command
						} else if (inputLine.startsWith("dl")) {
							// TODO: Finish this

							// Send a list of commands if the client sends the "help" command
						}
					}
				} else {
					// Close the connection if the response to the challenge was incorrect
					Terminal.print(clientSocket.getInetAddress() + " has sent an incorrect password. Closing connection.");
					streams.send("You have sent an incorrect password. Closing connection.");
					streams.closeStreams();
					clientSocket.close();
				}
			} else {
				// End the connection if the client fails the key exchange
				Terminal.print("Client at " + clientSocket.getInetAddress()
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
		PORT = Integer.parseInt(Terminal.ask("Enter the port number: "));
		FOLDER = new File(Terminal.ask("Enter the folder that you would like to share: "));

		// Setup the password
		try {
			String password = Terminal.ask("Enter in a password: ");
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			passwordHash = Base64.getEncoder().encodeToString(md.digest(password.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			Terminal.error("Error while hashing the password.");
			Terminal.error(e);
		}
	}
}
