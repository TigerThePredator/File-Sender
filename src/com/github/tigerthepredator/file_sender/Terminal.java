package com.github.tigerthepredator.file_sender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.Key;
import java.util.Scanner;

public class Terminal {
	private static final Scanner SCAN = new Scanner(System.in);
	private PrintWriter out;
	private BufferedReader in;

	public Terminal(Socket socket) {
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			out = null;
			in = null;
			error("Error while initializing streams");
			error(e.getMessage());
		}
	}

	public void send(String message, Key publicKey) {
		out.print(Encryptor.encrypt(message, publicKey));
		out.flush();
	}

	public void sendUnencrypted(String message) {
		out.print(message);
		out.flush();
	}

	public String receive(Key privateKey) {
		try {
			return Encryptor.decrypt(in.readLine(), privateKey);
		} catch (IOException e) {
			error("Error while receiving message");
			error(e.getMessage());
		}
		return null;
	}

	public String receiveUnencrypted() {
		try {
			return in.readLine();
		} catch (IOException e) {
			error("Error while receiving unencrypted message.");
			error(e.getMessage());
		}
		return null;
	}

	public static void print(String line) {
		System.out.println("\033[37m" + line);
	}

	public static void confirm(String line) {
		System.out.println("\033[32m" + line);
	}

	public static void error(String line) {
		System.out.println("\033[31m" + line);
	}

	public static String ask(String line) {
		System.out.print("\033[36m" + line);
		return scanLine();
	}

	public static String scanLine() {
		return SCAN.nextLine();
	}

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
