package com.imagemeta.util.i18n;

import com.imagemeta.util.logging.Logger;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * {@summary Lightweight, file-backed user preferences using {@link Properties}.}
 * <p>
 * Stores settings at {@code ~/.imagemeta/preferences.properties}. Loads once on class
 * init and persists on each {@link #set(String, String)} or {@link #setBoolean(String, boolean)}.
 * Uses standard Java properties encoding/format as defined by the JDK. :contentReference[oaicite:3]{index=3}
 */
public final class UserPreferences {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.imagemeta";
    private static final String PREFS_FILE = CONFIG_DIR + "/preferences.properties";
    private static final Properties props = new Properties();
    
    static {
        load();
    }

    private UserPreferences() {}

    /**
     * Loads preferences from disk if present; errors are logged but non-fatal.
     */
    private static void load() {
        try {
            Path path = Paths.get(PREFS_FILE);
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    props.load(in);
                    Logger.debug("Loaded preferences from: {}", PREFS_FILE);
                }
            }
        } catch (IOException e) {
            Logger.error("Failed to load preferences", e);
        }
    }

    /**
     * Persists preferences to {@code ~/.imagemeta/preferences.properties}, creating
     * the directory if needed.
     */
    public static void save() {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            try (OutputStream out = Files.newOutputStream(Paths.get(PREFS_FILE))) {
                props.store(out, "ImageMetadataManager Preferences");
            }
        } catch (IOException e) {
            Logger.error("Failed to save preferences", e);
        }
    }

    /** @return the value for {@code key}, or {@code def} if not present. */
    public static String get(String key, String def) {
        return props.getProperty(key, def);
    }

    /**
     * Sets a string preference and saves to disk.
     *
     * @param key   preference key
     * @param value preference value
     */
    public static void set(String key, String value) {
        props.setProperty(key, value);
        save();
    }

    /**
     * Reads a boolean preference with default.
     *
     * @param key preference key
     * @param def default value when absent
     * @return parsed boolean or {@code def}
     */
    public static boolean getBoolean(String key, boolean def) {
        String val = props.getProperty(key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    /**
     * Sets a boolean preference and saves to disk.
     *
     * @param key   preference key
     * @param value boolean value
     */
    public static void setBoolean(String key, boolean value) {
        set(key, String.valueOf(value));
    }
}
