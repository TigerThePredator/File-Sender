package com.github.tigerthepredator.file_sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.Key;
import java.util.Scanner;

public class Terminal {
	private static final Scanner SCAN = new Scanner(System.in); // Scanner for scanning command line
	private PrintWriter out; // Used to send data to server
	private BufferedReader in; // Used to read data from server

	// Constructor
	public Terminal(Socket socket) {
		try {
			// Create I/O streams
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			out = null;
			in = null;
			error("Error while initializing streams");
			error(e.getMessage());
		}
	}

	// Send an encrypted message to the server
	public void send(String message, Key publicKey) {
		out.print(Encryptor.encrypt(message, publicKey));
		out.flush();
	}

	// Send an unencrypted message to the server
	public void sendUnencrypted(String message) {
		out.print(message);
		out.flush();
	}

	// Receive an encrypted message from the server
	public String receive(Key privateKey) {
		try {
			return Encryptor.decrypt(in.readLine(), privateKey);
		} catch (IOException e) {
			error("Error while receiving message");
			error(e.getMessage());
		}
		return null;
	}

	// Receive an unencrypted message from the server
	public String receiveUnencrypted() {
		try {
			return in.readLine();
		} catch (IOException e) {
			error("Error while receiving unencrypted message.");
			error(e.getMessage());
		}
		return null;
	}

	// Print a line in white color
	public static void print(String line) {
		System.out.println("\033[37m" + line);
	}

	// Print a line in green color
	public static void confirm(String line) {
		System.out.println("\033[32m" + line);
	}

	// Print a line in red color
	public static void error(String line) {
		System.out.println("\033[31m" + line);
	}

	// Print a line in blue color
	public static String ask(String line) {
		System.out.print("\033[36m" + line);
		return scanLine();
	}

	// Scan what string the user has typed in
	public static String scanLine() {
		return SCAN.nextLine();
	}

	// Scan what int the user has typed in
	public static int scanInt() {
		return SCAN.nextInt();
	}

	public static void close() {
		SCAN.close();
	}

	public void closeStreams() {
		try {
			in.close();
			out.close();
		} catch (IOException e) {
			error("Error while closing streams");
			error(e.getMessage());
		}
	}
}
