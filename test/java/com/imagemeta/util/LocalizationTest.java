package com.imagemeta.util.i18n;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;

class LocalizationTest {
    
    @AfterEach
    void cleanup() throws IOException {
        // Reset preferences
        Path prefsDir = Paths.get(System.getProperty("user.home"), ".imagemeta");
        if (Files.exists(prefsDir)) {
            Files.walk(prefsDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try { Files.delete(path); } catch (IOException e) {}
                });
        }
    }
    
    @Test
    void testGetSupportedLocales() {
        List<Locale> supported = Localization.getSupported();
        assertFalse(supported.isEmpty());
        assertTrue(supported.contains(Locale.ENGLISH));
        assertTrue(supported.contains(Locale.FRENCH));
    }
    
    @Test
    void testTranslateEnglishKey() {
        Localization.setLocale(Locale.ENGLISH);
        String translated = Localization.tr("app.title");
        assertNotNull(translated);
        assertFalse(translated.startsWith("!"));
    }
    
    @Test
    void testTranslateFrenchKey() {
        Localization.setLocale(Locale.FRENCH);
        String translated = Localization.tr("app.title");
        assertNotNull(translated);
        assertTrue(translated.contains("Gestionnaire"));
    }
    
    @Test
    void testTranslateMissingKey() {
        String result = Localization.tr("nonexistent.key");
        assertEquals("!nonexistent.key!", result);
    }
    
    @Test
    void testTranslateWithArgs() {
        Localization.setLocale(Locale.ENGLISH);
        String result = Localization.tr("msg.delete_confirm", 5);
        assertTrue(result.contains("5"));
    }
    
    @Test
    void testSetLocale() {
        Localization.setLocale(Locale.FRENCH);
        assertEquals(Locale.FRENCH, Localization.getCurrent());
    }
    
    @Test
    void testGetCurrentLocale() {
        assertNotNull(Localization.getCurrent());
    }
}
