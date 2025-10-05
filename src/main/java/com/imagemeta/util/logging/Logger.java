package com.imagemeta.util.logging;

import com.imagemeta.util.terminal.AnsiColors;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * {@summary Simple, asynchronous logger with console and rolling-file output.}
 * <p>
 * Features:
 * <ul>
 *   <li>Level-based logging to stdout/stderr with optional ANSI color.</li>
 *   <li>Single-threaded, non-blocking file writes via {@link ExecutorService}.</li>
 *   <li>ISO-like timestamping via {@link DateTimeFormatter}.</li>
 *   <li>Caller location derived from the current thread stack.</li>
 * </ul>
 * Color output is skipped when a console is not present (e.g., IDE launchers), where
 * {@code System.console()} may be {@code null}. :contentReference[oaicite:5]{index=5}
 * <p>
 * The single-thread executor provides ordered, sequential background writes. :contentReference[oaicite:6]{index=6}
 */
public final class Logger {
    /** Timestamp format used in log records (e.g., {@code 2025-01-15 12:34:56.789}). */
    private static final DateTimeFormatter TIMESTAMP = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private static LogLevel level = LogLevel.INFO;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static PrintWriter fileWriter;
    private static boolean colorEnabled = true;
    
    static {
        initLogFile();
        Runtime.getRuntime().addShutdownHook(new Thread(Logger::shutdown));
    }

    private Logger() {}

    /**
     * Initializes the log file in {@code logs/imagemeta.log}, creating parent directories.
     */
    private static void initLogFile() {
        try {
            Path logPath = Paths.get("logs/imagemeta.log");
            Files.createDirectories(logPath.getParent());
            fileWriter = new PrintWriter(new BufferedWriter(
                new FileWriter(logPath.toFile(), true)), true);
        } catch (IOException e) {
            System.err.println("Failed to init log: " + e.getMessage());
        }
    }

    /** Sets the global log level (messages below this are suppressed). */
    public static void setLevel(LogLevel newLevel) { level = newLevel; }
    /** Enables or disables ANSI colorization of console output. */
    public static void setColor(boolean enabled) { colorEnabled = enabled; }

    /** Logs a TRACE message if enabled. */ public static void trace(String msg, Object... args) { log(LogLevel.TRACE, msg, args); }
    /** Logs a DEBUG message if enabled. */ public static void debug(String msg, Object... args) { log(LogLevel.DEBUG, msg, args); }
    /** Logs an INFO message if enabled.  */ public static void info(String msg, Object... args) { log(LogLevel.INFO, msg, args); }
    /** Logs a WARN message if enabled.  */ public static void warn(String msg, Object... args) { log(LogLevel.WARN, msg, args); }
    /** Logs an ERROR message if enabled. */ public static void error(String msg, Object... args) { log(LogLevel.ERROR, msg, args); }
    
    /**
     * Logs an error with an attached {@link Throwable}. The stack trace is printed
     * to {@code System.err} only when the current {@link #level} is {@code DEBUG}
     * (or more verbose).
     *
     * @param msg message template
     * @param t   throwable to report
     */
    public static void error(String msg, Throwable t) {
        log(LogLevel.ERROR, msg + ": " + t.getMessage());
        if (level.getPriority() <= LogLevel.DEBUG.getPriority()) {
            t.printStackTrace();
        }
    }

    /**
     * Core logging routine: formats the record, prints to console, and enqueues
     * a background write to the log file.
     *
     * @param lvl severity
     * @param msg message template
     * @param args optional args for {@link String#format}
     */
    private static void log(LogLevel lvl, String msg, Object... args) {
        if (lvl.getPriority() < level.getPriority()) return;

        String formatted = args.length > 0 ? String.format(msg, args) : msg;
        String timestamp = LocalDateTime.now().format(TIMESTAMP);
        
        StackTraceElement caller = getCaller();
        String location = String.format("%s.%s:%d",
            caller.getClassName().substring(caller.getClassName().lastIndexOf('.') + 1),
            caller.getMethodName(),
            caller.getLineNumber());

        String entry = String.format("[%s] [%s] [%s] %s",
            timestamp, lvl.getLabel(), location, formatted);

        PrintStream out = lvl.getPriority() >= LogLevel.ERROR.getPriority() 
            ? System.err : System.out;
        
        if (colorEnabled && System.console() != null) {
            out.println(lvl.getColorCode() + entry + AnsiColors.RESET);
        } else {
            out.println(entry);
        }

        if (fileWriter != null) {
            executor.submit(() -> fileWriter.println(entry));
        }
    }

    /**
     * Attempts to identify the external caller (skipping logger frames).
     *
     * @return a {@link StackTraceElement} pointing to the calling site
     */
    private static StackTraceElement getCaller() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (int i = 3; i < stack.length; i++) {
            if (!stack[i].getClassName().equals(Logger.class.getName())) {
                return stack[i];
            }
        }
        return stack[3];
    }

    /**
     * Flushes and shuts down the background writer on JVM shutdown.
     */
    private static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (fileWriter != null) fileWriter.close();
    }
}
