package com.github.tigerthepredator.file_sender;

import java.security.NoSuchAlgorithmException;

public class Start {
    // Main method
    public static void main(String[] args) {
        // TODO: Setup config file instead

        // Ask the user whether they want to setup a server or client
        int option = Integer.parseInt(
                Logger.ask("1.) Create data server.\n2.) Connect to data server.\nPlease select an option: "));
        try {
            if (option == 1) { // Setup a server if they choose option one
                Server s = new Server();
                Logger.log("Starting server.");
                s.connection();
            } else if (option == 2) { // Setup a client if they choose option two
                Client c = new Client();
                c.commandLine();
            }
        } catch (java.io.IOException | NoSuchAlgorithmException e) {
            Logger.error("Error while running the connection.");
            Logger.error(e);
        }

        // Close the scanner
        Logger.close();
    }
}
