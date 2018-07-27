package com.github.tigerthepredator.file_sender;

import java.io.IOException;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;

public class Client {

	private final Socket SOCKET;
	private final KeyPair KEYS;
	private final Key SERVER_KEY;
	private Terminal terminal;

	public void commandLine() {

	}

	private void download() {

	}

	public Client() throws IOException {
		KEYS = Encryptor.generateKeyPair();

		if (KEYS == null) {
			Terminal.error("Fatal error while generating keys. Exiting program...");
			System.exit(1);
		}

		String ip = Terminal.ask("Enter the server's IP address: ");

		int port = Integer.parseInt(Terminal.ask("Enter the port number: "));

		SOCKET = new Socket(ip, port);
		terminal = new Terminal(SOCKET);

		terminal.sendUnencrypted(Encryptor.keyToString(KEYS.getPublic()));
		String s = terminal.receiveUnencrypted();
		Terminal.print(s);
		SERVER_KEY = Encryptor.stringToKey(s);

		String password = Terminal.ask("Enter the password: ");
	}

}
