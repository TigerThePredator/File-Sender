package com.github.tigerthepredator.file_sender;

import java.security.NoSuchAlgorithmException;

public class Start {
	public static void main(String[] args) {
		int option = Integer.parseInt(Terminal.ask("1.)Create sftp server.\n2.)Connect to sftp server.\nPlease select an option: "));
		try {
			if (option == 1) {
				Server s = new Server();
				s.waitForConnection();
			} else if (option == 2) {
				Client c = new Client();
				c.commandLine();
			}
		} catch (java.io.IOException | NoSuchAlgorithmException e) {
			Terminal.error("Error while running the connection.");
			Terminal.error(e.getMessage());
		}

		Terminal.close();
	}
}
