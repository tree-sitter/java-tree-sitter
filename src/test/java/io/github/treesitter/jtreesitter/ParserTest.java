package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import java.nio.charset.StandardCharsets;
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
    void setLogger() {
        assertSame(parser, parser.setLogger(null));
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
        var encoding = InputEncoding.valueOf(StandardCharsets.UTF_16);
        try (var tree = parser.parse("var java = \"💩\";", encoding).orElseThrow()) {
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
        ParseCallback callback = (offset, _) -> source.substring(offset, Integer.min(offset, source.length()));
        parser.setLanguage(language);
        try (var tree = parser.parse(callback, InputEncoding.UTF_8).orElseThrow()) {
            assertNull(tree.getText());
            assertEquals("program", tree.getRootNode().getType());
        }
    }

    @Test
    @DisplayName("parse(timeout)")
    @SuppressWarnings("deprecation")
    void parseTimeout() {
        var source = "}".repeat(1024);
        ParseCallback callback = (offset, _) -> source.substring(offset, Integer.min(source.length(), offset + 1));

        parser.setLanguage(language).setTimeoutMicros(2L);
        assertTrue(parser.parse(callback, InputEncoding.UTF_8).isEmpty());
    }

    @Test
    @DisplayName("parse(cancellation)")
    @SuppressWarnings("deprecation")
    void parseCancellation() {
        var source = "}".repeat(1024 * 1024);
        ParseCallback callback = (offset, _) -> source.substring(offset, Integer.min(source.length(), offset + 1));

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
            var result = service.submit(() -> parser.parse(callback, InputEncoding.UTF_8));
            assertTrue(result.get(30L, TimeUnit.MILLISECONDS).isEmpty());
        } catch (InterruptedException | CancellationException | ExecutionException | TimeoutException e) {
            fail("Parsing was not halted gracefully", e);
        }
    }

    @Test
    @DisplayName("parse(options)")
    void parseOptions() {
        var source = "}".repeat(1024);
        ParseCallback callback = (offset, _) -> source.substring(offset, Integer.min(source.length(), offset + 1));
        var options = new Parser.Options((state) -> state.getCurrentByteOffset() >= 1000);

        parser.setLanguage(language);
        assertTrue(parser.parse(callback, InputEncoding.UTF_8, options).isEmpty());
    }

    @Test
    void reset() {
        var source = "class foo bar() {}";
        ParseCallback callback = (offset, _) -> source.substring(offset, Integer.min(source.length(), offset + 1));
        var options = new Parser.Options(Parser.State::hasError);

        parser.setLanguage(language);
        parser.parse(callback, InputEncoding.UTF_8, options);
        parser.reset();
        try (var tree = parser.parse("String foo;").orElseThrow()) {
            assertFalse(tree.getRootNode().hasError());
        }
    }
}
