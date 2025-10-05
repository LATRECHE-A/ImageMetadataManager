package com.imagemeta.util.terminal;

/**
 * {@summary ANSI escape sequences and helpers for colorized terminal output.}
 * <p>
 * Provides common SGR color codes and a simple {@link #colorize(String, String)} utility
 * that respects an environment-based capability check. Most terminals use SGR 30–37 and
 * 90–97 for standard/bright foreground colors; reset is {@code ESC[0m}. :contentReference[oaicite:7]{index=7}
 */
public final class AnsiColors {
    /** Reset all attributes. */ public static final String RESET = "\u001B[0m";
    /** Standard 3-bit colors. */ 
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    /** Intensity and bright variants. */ 
    public static final String BOLD = "\u001B[1m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";

    /**
     * Whether color is enabled for the current environment. Heuristics:
     * <ul>
     *   <li>Windows: enable on version 10+ (modern consoles support ANSI).</li>
     *   <li>Unix-like: enable when {@code TERM} contains {@code color} or {@code xterm}.</li>
     * </ul>
     * (This is conservative and may be overridden via {@link #setEnabled(boolean)}.)
     */
    private static boolean enabled = detectSupport();

    private AnsiColors() {}

    /**
     * Detects basic terminal color support using OS and {@code TERM}.
     * <p>
     * Note: Some environments (IDEs, redirected output) may not provide a real console.
     * In such cases, explicit enabling may still be suppressed by the caller. :contentReference[oaicite:8]{index=8}
     */
    private static boolean detectSupport() {
        String term = System.getenv("TERM");
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            try {
                String ver = System.getProperty("os.version");
                return Integer.parseInt(ver.split("\\.")[0]) >= 10;
            } catch (Exception e) {
                return false;
            }
        }
        
        return term != null && (term.contains("color") || term.contains("xterm"));
    }

    /**
     * Wraps {@code text} with the given ANSI color code if enabled.
     *
     * @param text  raw text
     * @param color an SGR foreground code (e.g., {@code \u001B[31m} for red)
     * @return colorized or plain text depending on {@link #isEnabled()}
     */
    public static String colorize(String text, String color) {
        return enabled ? color + text + RESET : text;
    }

    /** Globally enables or disables colorization. */
    public static void setEnabled(boolean enable) { enabled = enable; }
    /** @return whether colorization is currently enabled. */
    public static boolean isEnabled() { return enabled; }
}
