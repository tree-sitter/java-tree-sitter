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
    void getVersion() {
        assertEquals(14, language.getVersion());
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
    void query() {
        assertDoesNotThrow(() -> language.query("(identifier) @ident").close());
    }

    @Test
    void testEquals() {
        var other = new Language(TreeSitterJava.language());
        assertEquals(other, language.clone());
    }
}
