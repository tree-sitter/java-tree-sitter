package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class ParserTest {
    private static Language language;
    private Parser parser;

    @BeforeAll
    static void beforeAll() {
        language = new Language(TreeSitterJava.language());
    }

    @BeforeEach
    void setUp() {
        parser = new Parser();
    }

    @AfterEach
    void tearDown() {
        parser.close();
    }

    @Test
    void getLanguage() {
        assertNull(parser.getLanguage());
    }

    @Test
    void setLanguage() {
        assertSame(parser, parser.setLanguage(language));
        assertEquals(language, parser.getLanguage());
    }

    @Test
    void getTimeoutMicros() {
        assertEquals(0L, parser.getTimeoutMicros());
    }

    @Test
    void setTimeoutMicros() {
        assertSame(parser, parser.setTimeoutMicros(10L));
        assertEquals(10L, parser.getTimeoutMicros());
    }

    @Test
    void setLogger() {
        assertSame(parser, parser.setLogger(null));
    }

    @Test
    void setCancellationFlag() {
        var flag = new Parser.CancellationFlag();
        assertSame(parser, parser.setCancellationFlag(flag));
    }

    @Test
    void getIncludedRanges() {
        assertEquals(1, parser.getIncludedRanges().size());
    }

    @Test
    void setIncludedRanges() {
        var range = new Range(Point.MIN, new Point(0, 1), 0, 1);
        assertSame(parser, parser.setIncludedRanges(List.of(range)));
        assertIterableEquals(List.of(range), parser.getIncludedRanges());
        assertThrows(IllegalArgumentException.class, () -> parser.setIncludedRanges(List.of(range, range)));
    }

    @Test
    @DisplayName("parse(utf8)")
    void parseUtf8() {
        parser.setLanguage(language);
        try (var tree = parser.parse("class Foo {}").orElseThrow()) {
            var rootNode = tree.getRootNode();

            assertEquals(12, rootNode.getEndByte());
            assertFalse(rootNode.isError());
            assertEquals("(program (class_declaration name: (identifier) body: (class_body)))", rootNode.toSexp());
        }
    }

    @Test
    @DisplayName("parse(utf16)")
    void parseUtf16() {
        parser.setLanguage(language);
        try (var tree = parser.parse("var java = \"💩\";", InputEncoding.UTF_16).orElseThrow()) {
            var rootNode = tree.getRootNode();

            assertEquals(32, rootNode.getEndByte());
            assertFalse(rootNode.isError());
            assertEquals(
                    "(program (local_variable_declaration type: (type_identifier) declarator: (variable_declarator name: (identifier) value: (string_literal (string_fragment)))))",
                    rootNode.toSexp());
        }
    }

    @Test
    @DisplayName("parse(logger)")
    void parseLogger() {
        var messages = new ArrayList<String>();
        parser.setLanguage(language)
                .setLogger((type, message) -> messages.add("%s - %s".formatted(type.name(), message)))
                .parse("class Foo {}")
                .orElseThrow()
                .close();
        assertEquals(44, messages.size());
        assertEquals("LEX - new_parse", messages.getFirst());
        assertEquals("PARSE - consume character:'c'", messages.get(3));
        assertEquals("LEX - done", messages.getLast());
    }

    @SuppressWarnings("unused")
    @Test
    @DisplayName("parse(callback)")
    void parseCallback() {
        var source = "class Foo {}";
        // NOTE: can't use _ because of palantir/palantir-java-format#934
        ParseCallback callback = (offset, p) -> source.substring(offset, Integer.min(offset, source.length()));
        parser.setLanguage(language);
        try (var tree = parser.parse(callback, InputEncoding.UTF_8).orElseThrow()) {
            assertNull(tree.getText());
            assertEquals("program", tree.getRootNode().getType());
        }
    }

    @Test
    @DisplayName("parse(timeout)")
    void parseTimeout() {
        assertThrows(IllegalStateException.class, () -> parser.parse(""));
        parser.setLanguage(language).setTimeoutMicros(2L);
        assertTrue(parser.parse("}".repeat(1024), InputEncoding.UTF_8).isEmpty());
    }

    @Test
    @DisplayName("parse(cancellation)")
    void parseCancellation() {
        var flag = new Parser.CancellationFlag();
        parser.setLanguage(language).setCancellationFlag(flag);
        try (var service = Executors.newFixedThreadPool(2)) {
            service.submit(() -> {
                try {
                    wait(10L);
                } catch (InterruptedException e) {
                    service.shutdownNow();
                } finally {
                    flag.set(1L);
                }
            });
            var result = service.submit(() -> parser.parse("}".repeat(1024 * 1024)));
            assertTrue(result.get(30L, TimeUnit.MILLISECONDS).isEmpty());
        } catch (InterruptedException | CancellationException | ExecutionException | TimeoutException e) {
            fail("Parsing was not halted gracefully", e);
        }
    }

    @Test
    void reset() {
        parser.setLanguage(language).setTimeoutMicros(1L);
        parser.parse("{".repeat(1024));
        parser.reset();
        try (var tree = parser.parse("String foo;").orElseThrow()) {
            assertFalse(tree.getRootNode().hasError());
        }
    }
}
