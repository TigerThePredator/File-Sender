package com.github.tigerthepredator.file_sender;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client {
	private final Socket SOCKET; // Socket used for connection
	private final KeyPair KEYS; // The client's public/private key pair
	private final Key SERVER_KEY; // The server's public key
	private Terminal terminal; // The terminal used to send messages

	// Used to send commands to the server
	public void commandLine() {
		// Loop until the client closes the connection
		boolean exit = false;
		while (!exit) {
			// Client should be able to type in a command
			Terminal.print("> ");
			String command = Terminal.scanLine();
		}
	}

	// Constructor
	public Client() throws IOException, NoSuchAlgorithmException {
		// Generate a key pair
		KEYS = Encryptor.generateKeyPair();
		if (KEYS == null) {
			Terminal.error("Fatal error while generating keys. Exiting program...");
			System.exit(1);
		}

		// TODO: Ask for information via command line arguments

		// Ask for information about the server
		String ip = Terminal.ask("Enter the server's IP address: ");
		int port = Integer.parseInt(Terminal.ask("Enter the port number: "));

		// Setup socket and terminal connections
		Terminal.print("Setting up connection with server...\n");
		SOCKET = new Socket(ip, port);
		terminal = new Terminal(SOCKET);
		Terminal.confirm("Successfully connected to server.");

		// Send our public key
		terminal.sendUnencrypted(Encryptor.keyToString(KEYS.getPublic()));

		// Receive the server's public key and make sure that it is not null
		SERVER_KEY = Encryptor.stringToKey(terminal.receiveUnencrypted());
		if (SERVER_KEY != null) {
			// Send the answer to the server's challenge
			// The server should send a random string as a challenge
			// The answer to the challenge is SHA256(random string + password)
			String password = Terminal.ask("Enter the password: ");
			String challenge = terminal.receive(KEYS.getPrivate()) + password;
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String encodedhash = Encryptor.bytesToHex(digest.digest(challenge.getBytes(StandardCharsets.UTF_8)));
			terminal.send(encodedhash, SERVER_KEY);
		} else {
			Terminal.error("The server did not submit a proper public key. Connection will be closed.");
			System.exit(1);
		}
	}

}
