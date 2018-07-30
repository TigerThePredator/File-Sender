package com.github.tigerthepredator.file_sender;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Scanner;

public class Terminal {
	private static final Scanner SCAN = new Scanner(System.in); // Scanner for scanning command line

	// Print a line in white color
	public static void print(String line) {
		System.out.print("\033[37m" + line);
	}

	// Print a line in green color
	public static void confirm(String line) {
		System.out.println("\033[32m" + line);
	}

	// Print a line in red color
	public static void error(String line) {
		System.out.println("\033[31m" + line);
	}
	
	// Print an error in red color
	public static void error(Exception e) {
		Writer result = new StringWriter();
		PrintWriter writer = new PrintWriter(result);
		e.printStackTrace(writer);
		System.out.println("\033[31m" + result.toString());
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

	// Close the scanner
	public static void close() {
		SCAN.close();
	}
}
