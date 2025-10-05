package com.imagemeta.util.logging;

/**
 * {@summary Log severity levels with printable labels and ANSI color hints.}
 * <p>
 * Colors map to SGR codes (e.g., 30–37 for standard foreground; 90–97 for bright variants),
 * which many terminals interpret for colored output. :contentReference[oaicite:4]{index=4}
 */
public enum LogLevel {
    TRACE(0, "TRACE", "\u001B[37m"),
    DEBUG(1, "DEBUG", "\u001B[36m"),
    INFO(2, "INFO", "\u001B[32m"),
    WARN(3, "WARN", "\u001B[33m"),
    ERROR(4, "ERROR", "\u001B[31m"),
    FATAL(5, "FATAL", "\u001B[35m");

    private final int priority;
    private final String label;
    private final String colorCode;

    LogLevel(int priority, String label, String colorCode) {
        this.priority = priority;
        this.label = label;
        this.colorCode = colorCode;
    }

    /** @return numeric priority; lower means more verbose. */
    public int getPriority() { return priority; }
    /** @return short printable label for the level. */
    public String getLabel() { return label; }
    /** @return ANSI SGR color sequence used by the console logger. */
    public String getColorCode() { return colorCode; }
}
