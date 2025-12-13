package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LanguageTest {
    private static Language language;

    @BeforeAll
    static void beforeAll() {
        language = new Language(TreeSitterJava.language());
    }

    @Test
    void getAbiVersion() {
        assertEquals(15, language.getAbiVersion());
    }

    @Test
    void getName() {
        assertEquals("java", language.getName());
    }

    @Test
    void getMetadata() {
        assertNotNull(language.getMetadata());
    }

    @Test
    void getSymbolCount() {
        assertEquals(321, language.getSymbolCount());
    }

    @Test
    void getStateCount() {
        assertEquals(1385, language.getStateCount());
    }

    @Test
    void getFieldCount() {
        assertEquals(40, language.getFieldCount());
    }

    @Test
    void getSymbolName() {
        assertEquals("identifier", language.getSymbolName((short) 1));
        assertNull(language.getSymbolName((short) 999));
    }

    @Test
    void getSymbolForName() {
        assertEquals((short) 138, language.getSymbolForName("program", true));
        assertEquals((short) 0, language.getSymbolForName("$", false));
    }

    @Test
    void getSupertypes() {
        short[] supertypes = {140, 263, 264, 216, 147, 219, 157, 185};
        assertArrayEquals(supertypes, language.getSupertypes());
    }

    @Test
    void getSubtypes() {
        assertEquals(21, language.getSubtypes((short) 185).length);
    }

    @Test
    void isNamed() {
        assertTrue(language.isNamed((short) 1));
    }

    @Test
    void isVisible() {
        assertTrue(language.isVisible((short) 1));
    }

    @Test
    void isSupertype() {
        assertFalse(language.isSupertype((short) 1));
    }

    @Test
    void getFieldNameForId() {
        assertNotNull(language.getFieldNameForId((short) 20));
    }

    @Test
    void getFieldIdForName() {
        assertEquals(20, language.getFieldIdForName("name"));
    }

    @Test
    void nextState() {
        assertNotEquals(0, language.nextState((short) 1, (short) 138));
    }

    @Test
    void lookaheadIterator() {
        assertDoesNotThrow(() -> {
            var state = language.nextState((short) 1, (short) 138);
            language.lookaheadIterator(state).close();
        });
    }

    @Test
    void testEquals() {
        var other = new Language(TreeSitterJava.language());
        assertEquals(other, language.clone());
    }
}
