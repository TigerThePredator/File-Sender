package com.github.tigerthepredator.file_sender;

import java.security.NoSuchAlgorithmException;

public class Start {
	// Main method
	public static void main(String[] args) {
		// TODO: Setup command line arguments instead
		
		// Ask the user whether they want to setup a server or client
		int option = Integer.parseInt(Terminal.ask("1.) Create data server.\n2.) Connect to data server.\nPlease select an option: "));
		try {
			if (option == 1) { // Setup a server if they choose option one
				Server s = new Server();
				s.connection();
			} else if (option == 2) { // Setup a client if they choose option two
				Client c = new Client();
				Terminal.print("You should now be able to type in commands :)\n");
				c.commandLine();
			}
		} catch (java.io.IOException | NoSuchAlgorithmException e) {
			Terminal.error("Error while running the connection.");
			Terminal.error(e);
		}

		// Close the scanner
		Terminal.close();
	}
}
