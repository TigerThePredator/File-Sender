package com.github.tigerthepredator.file_sender;

import java.io.IOException;
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

	// Used to send commands to the server
	public void commandLine() {
		// Confirm that you have successfully connected to the server
		Terminal.confirm("Successfully connected to server.\n");
		Terminal.confirm("You should now be able to type in commands :)\n");

		// Loop until the client closes the connection
		boolean exit = false;

		// Loop until the client closes the connection
		while (!exit) {
			// Client should be able to send in commands
			Terminal.print("> ");
			String command = Terminal.scanLine().trim();

			// If the client sends the "exit" command, close the connection
			if (command.startsWith("exit")) {
				streams.send(command);
				Terminal.print("Closing connection.\n");
				exit = true;
				Terminal.close();
				streams.closeStreams();
			// If the client sends the "ls" command, print out a list of files and folders
			} else if(command.startsWith("ls")) {
				streams.send(command);
				int numberOfFiles = Integer.parseInt(streams.receive());
				for(int x = 0; x < numberOfFiles; x++)
					Terminal.print(streams.receive());
				
			// Do nothing if the user typed in nothing
			} else if(command.trim().equals("")) {
				// Do nothing
			// If the client sends the "help" command, print out a list of commands
			} else if(command.startsWith("help")) {
				// TODO: Print out a list of commands
			// If the client sends the "cd" command, change the directory
			} else if(command.startsWith("cd")) {
				streams.send(command);
				// TODO: Figure out why there is an error with the cd command
			// Else, state that the client did not send a usable command
			} else {
				Terminal.print("\'" + command + "\' is not a usable command.\n");
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
		String ip = Terminal.ask("Enter the server's IP address: ");
		int port = Integer.parseInt(Terminal.ask("Enter the port number: "));

		// Setup socket and terminal connections
		Terminal.print("Setting up connection with server...\n");
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
				String password = Terminal.ask("Enter the password: ");
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
				Terminal.print(challengeResponse + "\n");
				if (!challengeResponse.equals("Correct password.")) {
					streams.closeStreams();
					Terminal.close();
					System.exit(0);
				}
			} else {
				// Close the connection if the server did not submit a proper key
				Terminal.error("The server did not submit a proper key. Connection will be closed.");
				streams.closeStreams();
				Terminal.close();
				System.exit(1);
			}
		} catch (NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			Terminal.error("Error while doing key exchange and password challenge.");
			Terminal.error(e);
		}
	}

}
