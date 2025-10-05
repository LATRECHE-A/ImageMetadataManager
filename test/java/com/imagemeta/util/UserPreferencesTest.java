package com.imagemeta.util.i18n;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.Comparator;

class UserPreferencesTest {
    
    @AfterEach
    void cleanup() throws IOException {
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
    void testSetAndGet() {
        UserPreferences.set("test.key", "test.value");
        assertEquals("test.value", UserPreferences.get("test.key", "default"));
    }
    
    @Test
    void testGetWithDefault() {
        String value = UserPreferences.get("nonexistent.key", "default");
        assertEquals("default", value);
    }
    
    @Test
    void testSetAndGetBoolean() {
        UserPreferences.setBoolean("test.bool", true);
        assertTrue(UserPreferences.getBoolean("test.bool", false));
    }
    
    @Test
    void testGetBooleanWithDefault() {
        boolean value = UserPreferences.getBoolean("nonexistent.bool", true);
        assertTrue(value);
    }
    
    @Test
    void testPersistence() {
        UserPreferences.set("persist.test", "value");
        UserPreferences.save();
        
        String retrieved = UserPreferences.get("persist.test", "default");
        assertEquals("value", retrieved);
    }
}
