package file_sender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Logger {
    private static final Scanner SCAN = new Scanner(System.in); // Scanner for scanning command line
    private static File log; // File for logging data
    private static PrintWriter logWriter; // Writer for log

    static {
        try {
            // Initialize the log file
            log = File.createTempFile("file_sender", ".log");
            logWriter = new PrintWriter(log);
            print("Log file created in " + log.getAbsolutePath() + ".\n");
            
            // TODO: Allow the user to change where the log file is located
            
            /* For now we will just place the log file in the /tmp/ directory
             * but in the future, the user should be able to change where the
             * log file is located himself. */
        } catch (IOException e) {
            error("Error while initializing log file.");
            error(e);
        }
    }
    
    // Print a line in blue color
    public static void print(String line) {
        System.out.print("\033[36m" + line);
        log("Printed message:\n" + line);
    }

    // Print a line in red color
    public static void error(String line) {
        System.out.println("\033[31m" + line);
        log("Error message:\n" + line);
    }

    // Print an error in red color
    public static void error(Exception e) {
        Writer result = new StringWriter();
        PrintWriter writer = new PrintWriter(result);
        e.printStackTrace(writer);
        System.out.println("\033[31m" + result.toString());
        log("Exception caught:\n" + result.toString());
    }

    // Print a question in blue color
    public static String ask(String line) {
        log("Printed question:\n" + line);
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

    // Change the location of the log files
    public static void changeLogFile(File f) {
        try {
            log = f;
            logWriter = new PrintWriter(log);
            // TODO: Move data in original log file to new log file
        } catch (FileNotFoundException e) {
            error("Error while changing log file.");
            error(e);
        }
    }
    
    // Log something
    public static void log(String s) {
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        Date dateobj = new Date();
        logWriter.println(s.trim() + "\n@ " + df.format(dateobj));
        logWriter.println();
        logWriter.flush();
        // TODO: Encrypt log files
    }
    
    // Close the scanner and log writer
    public static void close() {
        log("Closing scanner.");
        SCAN.close();
        log("Closing log writer.");
        logWriter.close();
    }
}
