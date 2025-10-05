package com.imagemeta.util.i18n;

import com.imagemeta.util.logging.Logger;
import java.util.*;

/**
 * {@summary Centralized localization manager for the application.}
 * <p>
 * Handles initial locale resolution (saved preference → system default → English),
 * exposes the current {@link Locale}, and provides simple translation helpers backed by
 * a {@link ResourceBundle}. Missing bundles/keys are logged and gracefully handled.
 * <p>
 * Resource bundles are loaded from the base name {@code i18n.messages} using
 * {@link ResourceBundle#getBundle(String, Locale)}. :contentReference[oaicite:1]{index=1}
 */
public final class Localization {
    /** Preference key storing the language tag (e.g., {@code en}, {@code fr}). */
    private static final String PREF_LANG = "language";
    /** Preference key indicating the language was chosen/configured. */
    private static final String PREF_CONFIGURED = "language.configured";
    /** Supported locales (language-only matching). */
    private static final List<Locale> SUPPORTED = Arrays.asList(
        Locale.ENGLISH,
        Locale.FRENCH,
        new Locale("es")
    );
    
    /** The currently active locale. */
    private static Locale current;
    /** The active resource bundle used by {@link #tr(String)}. */
    private static ResourceBundle bundle;

    static {
        initialize();
    }

    private Localization() {}

    /**
     * Initializes the current locale and resource bundle.
     * <p>
     * If a saved language exists, it is preferred; otherwise the system default is used
     * when supported; otherwise English is selected and the UI may prompt the user later.
     */
    private static void initialize() {
        boolean configured = UserPreferences.getBoolean(PREF_CONFIGURED, false);
        
        if (configured) {
            String saved = UserPreferences.get(PREF_LANG, null);
            if (saved != null) {
                current = Locale.forLanguageTag(saved);
                Logger.info("Using saved language: {}", current);
            }
        } else {
            Locale system = Locale.getDefault();
            if (isSupported(system)) {
                current = system;
                Logger.info("Detected system language: {}", current);
            } else {
                current = Locale.ENGLISH;
                Logger.info("System language not supported, will prompt");
            }
        }
        
        loadBundle();
    }

    /**
     * @return {@code true} if the application should prompt for language selection.
     */
    public static boolean needsSelection() {
        return !UserPreferences.getBoolean(PREF_CONFIGURED, false);
    }

    /**
     * Sets the application locale, persists it to preferences, and reloads the bundle.
     * <p>
     * Unsupported locales fall back to English (with a warning).
     *
     * @param locale requested locale
     */
    public static void setLocale(Locale locale) {
        if (!isSupported(locale)) {
            Logger.warn("Unsupported locale: {}", locale);
            locale = Locale.ENGLISH;
        }
        
        current = locale;
        UserPreferences.set(PREF_LANG, locale.toLanguageTag());
        UserPreferences.setBoolean(PREF_CONFIGURED, true);
        loadBundle();
    }

    /** @return the current {@link Locale}. */
    public static Locale getCurrent() { return current; }
    
    /** @return an unmodifiable list of supported locales. */
    public static List<Locale> getSupported() {
        return Collections.unmodifiableList(SUPPORTED);
    }

    /**
     * @param locale a locale to test
     * @return {@code true} if its language is among {@link #SUPPORTED}.
     */
    private static boolean isSupported(Locale locale) {
        return SUPPORTED.stream()
            .anyMatch(s -> s.getLanguage().equals(locale.getLanguage()));
    }

    /**
     * Loads the {@link ResourceBundle} for the current locale, defaulting to English.
     * <p>
     * See the JDK docs for bundle resolution and fallback behavior. :contentReference[oaicite:2]{index=2}
     */
    private static void loadBundle() {
        try {
            bundle = ResourceBundle.getBundle("i18n.messages", current);
        } catch (MissingResourceException e) {
            Logger.warn("Bundle not found for {}, using English", current);
            bundle = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
        }
    }

    /**
     * Translates a key into the current locale.
     *
     * @param key bundle key
     * @return localized string, or a {@code !key!} placeholder if missing
     */
    public static String tr(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            Logger.warn("Missing key: {}", key);
            return "!" + key + "!";
        }
    }

    /**
     * Formats a translated string using {@link String#format(String, Object...)} semantics.
     *
     * @param key  bundle key
     * @param args arguments applied to the localized pattern
     * @return formatted localized string
     */
    public static String tr(String key, Object... args) {
        return String.format(tr(key), args);
    }
}
