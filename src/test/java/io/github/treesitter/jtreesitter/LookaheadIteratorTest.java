package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import java.util.List;
import org.junit.jupiter.api.*;

class LookaheadIteratorTest {
    private static Language language;
    private static short state;
    private LookaheadIterator lookahead;

    @BeforeAll
    static void beforeAll() {
        language = new Language(TreeSitterJava.language());
        state = language.nextState((short) 1, (short) 138);
    }

    @BeforeEach
    void setUp() {
        lookahead = language.lookaheadIterator(state);
    }

    @AfterEach
    void tearDown() {
        lookahead.close();
    }

    @Test
    void getLanguage() {
        assertEquals(language, lookahead.getLanguage());
    }

    @Test
    void getCurrentSymbol() {
        assertEquals((short) -1, lookahead.getCurrentSymbol());
    }

    @Test
    void getCurrentSymbolName() {
        assertEquals("ERROR", lookahead.getCurrentSymbolName());
    }

    @Test
    @DisplayName("reset(state)")
    void resetState() {
        assertDoesNotThrow(() -> lookahead.next());
        assertTrue(lookahead.reset(state));
        assertEquals("ERROR", lookahead.getCurrentSymbolName());
    }

    @Test
    @DisplayName("reset(language)")
    void resetLanguage() {
        assertDoesNotThrow(() -> lookahead.next());
        assertTrue(lookahead.reset(state, language));
        assertEquals("ERROR", lookahead.getCurrentSymbolName());
    }

    @Test
    void hasNext() {
        assertTrue(lookahead.hasNext());
        assertEquals("ERROR", lookahead.getCurrentSymbolName());
    }

    @Test
    void next() {
        assertEquals("end", lookahead.next().name());
    }

    @Test
    void symbols() {
        assertEquals(3, lookahead.symbols().count());
    }

    @Test
    void names() {
        var names = List.of("end", "line_comment", "block_comment");
        assertEquals(names, lookahead.names().toList());
    }
}
