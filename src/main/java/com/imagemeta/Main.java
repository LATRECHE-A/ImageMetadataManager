package com.imagemeta;

import com.imagemeta.ui.cli.ConsoleInterface;
import com.imagemeta.ui.gui.GraphicalInterface;
import com.imagemeta.util.logging.Logger;
import com.imagemeta.util.logging.LogLevel;

/**
 * Main entry point for ImageMetadataManager.
 * Supports both CLI and GUI modes with proper error handling.
 */
public class Main {
    
    static {
        // Initialize logging early
        Logger.setLevel(LogLevel.INFO);
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                Logger.info("Starting GUI mode (default)");
                GraphicalInterface.start();
                return;
            }

            String mode = args[0].toLowerCase();
            switch (mode) {
                case "--cli":
                case "cli":
                    Logger.info("Starting CLI mode");
                    String[] cliArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, cliArgs, 0, args.length - 1);
                    ConsoleInterface.start(cliArgs);
                    break;
                    
                case "--gui":
                case "gui":
                    Logger.info("Starting GUI mode");
                    GraphicalInterface.start();
                    break;
                    
                case "--help":
                case "-h":
                    printUsage();
                    break;
                    
                case "--version":
                case "-v":
                    printVersion();
                    break;
                    
                case "--debug":
                    Logger.setLevel(LogLevel.DEBUG);
                    Logger.debug("Debug mode enabled");
                    if (args.length > 1) {
                        String[] debugArgs = new String[args.length - 1];
                        System.arraycopy(args, 1, debugArgs, 0, args.length - 1);
                        main(debugArgs);
                    } else {
                        GraphicalInterface.start();
                    }
                    break;
                    
                default:
                    Logger.error("Unrecognized option: {}", args[0]);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            Logger.error("Fatal error in main", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("ImageMetadataManager - Manage and analyze image metadata");
        System.out.println("\nUsage: java -jar ImageMetadataManager.jar [OPTIONS] [ARGS]");
        System.out.println("\nOPTIONS:");
        System.out.println("  --gui, gui         Launch graphical interface (default)");
        System.out.println("  --cli, cli         Launch command-line interface");
        System.out.println("  --debug            Enable debug logging");
        System.out.println("  --version, -v      Show version information");
        System.out.println("  --help, -h         Show this help message");
        System.out.println("\nEXAMPLES:");
        System.out.println("  java -jar ImageMetadataManager.jar");
        System.out.println("  java -jar ImageMetadataManager.jar --cli --help");
        System.out.println("  java -jar ImageMetadataManager.jar --debug --gui");
    }

    private static void printVersion() {
        System.out.println("ImageMetadataManager v2.0.0");
        System.out.println("Java Runtime: " + System.getProperty("java.version"));
        System.out.println("Operating System: " + System.getProperty("os.name"));
    }
}
