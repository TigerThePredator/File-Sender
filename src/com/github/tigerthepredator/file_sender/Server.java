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
	private final int PORT;
	private final String PASSWORD;
	private final File FOLDER;
	private final KeyPair KEYS;

	public void waitForConnection() throws IOException, NoSuchAlgorithmException {
		ServerSocket serverSocket = new ServerSocket(PORT);
		while (true) {
			Socket clientSocket = serverSocket.accept();
			Terminal terminal = new Terminal(clientSocket);

			String s = terminal.receiveUnencrypted();
			Key clientKey = Encryptor.stringToKey(s);
			Terminal.print(s);
			terminal.send(Encryptor.keyToString(KEYS.getPublic()), clientKey);

			String challenge = Encryptor.randString(10);
			terminal.send(challenge, clientKey);
			challenge = challenge + PASSWORD;
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String encodedhash = Encryptor.bytesToHex(digest.digest(challenge.getBytes(StandardCharsets.UTF_8)));
			if (terminal.receive(KEYS.getPrivate()) != encodedhash) {
				terminal.send("You have sent an incorrect password. Closing connection", clientKey);
			} else {
				File currentFolder = FOLDER;

				String inputLine;

				while ((inputLine = terminal.receive(KEYS.getPrivate())) != null) {
					if (inputLine.startsWith("ls")) {
						for (String file : currentFolder.list())
							terminal.send(file + "\n", clientKey);
					} else if (inputLine.startsWith("cd")) {
						File newFolder = new File(currentFolder.getAbsolutePath() + inputLine.split(" ")[1]);
						if ((newFolder.exists()) && (newFolder.isDirectory())) {
							currentFolder = newFolder;
						} else
							terminal.send("The folder that you requested does not exist.", clientKey);
					} else {
						if (inputLine.equals("exit")) {
							Terminal.print("Client at " + clientSocket.getInetAddress().getHostAddress()
									+ " has disconnected");
							terminal.closeStreams();
							clientSocket.close();
							break;
						}
						if (!inputLine.startsWith("dl")) {
							inputLine.startsWith("help");
						}
					}
				}
			}
		}
	}

	public Server() {
		KEYS = Encryptor.generateKeyPair();

		if (KEYS == null) {
			Terminal.error("Fatal error while generating keys. Exiting program...");
			System.exit(1);
		}

		PORT = Integer.parseInt(Terminal.ask("Enter the port number: "));

		FOLDER = new File(Terminal.ask("Enter the folder that you would like to share: "));

		PASSWORD = Terminal.ask("Enter in a password: ");
	}
}
