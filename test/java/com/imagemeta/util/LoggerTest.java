package com.imagemeta.util.logging;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;

class LoggerTest {
    
    @Test
    void testSetLevel() {
        Logger.setLevel(LogLevel.DEBUG);
        assertDoesNotThrow(() -> Logger.debug("Test debug message"));
    }
    
    @Test
    void testLogMethods() {
        assertDoesNotThrow(() -> {
            Logger.trace("trace");
            Logger.debug("debug");
            Logger.info("info");
            Logger.warn("warn");
            Logger.error("error");
        });
    }
    
    @Test
    void testLogWithArgs() {
        assertDoesNotThrow(() -> Logger.info("Test {} {}", "arg1", "arg2"));
    }
    
    @Test
    void testLogWithException() {
        Exception ex = new Exception("test exception");
        assertDoesNotThrow(() -> Logger.error("Error occurred", ex));
    }
    
    @Test
    void testSetColorEnabled() {
        Logger.setColor(false);
        assertDoesNotThrow(() -> Logger.info("No color"));
        
        Logger.setColor(true);
        assertDoesNotThrow(() -> Logger.info("With color"));
    }
    
    @Test
    void testLogFileCreated() throws Exception {
        Logger.info("Test log entry");
        Thread.sleep(100); // Wait for async write
        
        Path logFile = Paths.get("logs/imagemeta.log");
        assertTrue(Files.exists(logFile));
    }
}
