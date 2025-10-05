package com.imagemeta.util.terminal;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class AnsiColorsTest {
    
    @Test
    void testColorize() {
        String colored = AnsiColors.colorize("test", AnsiColors.RED);
        assertNotNull(colored);
        assertTrue(colored.contains("test"));
    }
    
    @Test
    void testSetEnabled() {
        AnsiColors.setEnabled(false);
        assertFalse(AnsiColors.isEnabled());
        
        AnsiColors.setEnabled(true);
        assertTrue(AnsiColors.isEnabled());
    }
    
    @Test
    void testColorConstants() {
        assertNotNull(AnsiColors.RED);
        assertNotNull(AnsiColors.GREEN);
        assertNotNull(AnsiColors.BLUE);
        assertNotNull(AnsiColors.YELLOW);
        assertNotNull(AnsiColors.RESET);
    }
}
