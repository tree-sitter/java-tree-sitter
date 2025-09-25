package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class QueryTest {
    private static Language language;
    private static Parser parser;
    private static final String source =
            """
        (identifier) @identifier

        (class_declaration
            name: (identifier) @class
            (class_body) @body)
        """
                    .stripIndent();

    @BeforeAll
    static void beforeAll() {
        language = new Language(TreeSitterJava.language());
        parser = new Parser(language);
    }

    @AfterAll
    static void afterAll() {
        parser.close();
    }

    @SuppressWarnings("unused")
    private static void assertError(Class<? extends QueryError> type, String source, String message) {
        try (var _ = new Query(language, source)) {
            fail("Expected QueryError to be thrown, but nothing was thrown.");
        } catch (QueryError ex) {
            assertInstanceOf(type, ex);
            assertEquals(message, ex.getMessage());
        }
    }

    private static void assertQuery(Consumer<Query> assertions) {
        assertQuery(source, assertions);
    }

    private static void assertQuery(String source, Consumer<Query> assertions) {
        try (var query = new Query(language, source)) {
            assertions.accept(query);
        } catch (QueryError e) {
            fail("Unexpected query error", e);
        }
    }

    @Test
    void errors() {
        assertError(QueryError.Syntax.class, "(identifier) @", "Unexpected EOF");
        assertError(QueryError.Syntax.class, " identifier)", "Invalid syntax at row 0, column 1");

        assertError(
                QueryError.Capture.class,
                "((identifier) @foo\n (#test? @bar))",
                "Invalid capture name at row 1, column 10: bar");

        assertError(QueryError.NodeType.class, "(foo)", "Invalid node type at row 0, column 1: foo");

        assertError(QueryError.Field.class, "foo: (identifier)", "Invalid field name at row 0, column 0: foo");

        assertError(QueryError.Structure.class, "(program (identifier))", "Impossible pattern at row 0, column 9");

        assertError(
                QueryError.Predicate.class,
                "\n((identifier) @foo\n (#any-of?))",
                "Invalid predicate in pattern at row 1: #any-of? expects at least 2 arguments, got 0");
    }

    @Test
    void getPatternCount() {
        assertQuery(query -> assertEquals(2, query.getPatternCount()));
    }

    @Test
    void getCaptureNames() {
        assertQuery(query -> assertIterableEquals(List.of("identifier", "class", "body"), query.getCaptureNames()));
    }

    @Test
    void getStringValues() {
        var source = """
                ((identifier) @foo
                 (#eq? @foo "Foo"))
                """
                .stripIndent();
        assertQuery(source, query -> assertIterableEquals(List.of("eq?", "Foo"), query.getStringValues()));
    }

    @Test
    void disablePattern() {
        assertQuery(query -> {
            assertDoesNotThrow(() -> query.disablePattern(1));
            assertThrows(IndexOutOfBoundsException.class, () -> query.disablePattern(2));
        });
    }

    @Test
    void disableCapture() {
        assertQuery(query -> {
            assertDoesNotThrow(() -> query.disableCapture("body"));
            assertThrows(NoSuchElementException.class, () -> query.disableCapture("none"));
        });
    }

    @Test
    void startByteForPattern() {
        assertQuery(query -> {
            assertEquals(26, query.startByteForPattern(1));
            assertThrows(IndexOutOfBoundsException.class, () -> query.startByteForPattern(2));
        });
    }

    @Test
    void endByteForPattern() {
        assertQuery(query -> {
            assertEquals(26, query.endByteForPattern(0));
            assertThrows(IndexOutOfBoundsException.class, () -> query.endByteForPattern(2));
        });
    }

    @Test
    void isPatternRooted() {
        assertQuery(query -> {
            assertTrue(query.isPatternRooted(0));
            assertThrows(IndexOutOfBoundsException.class, () -> query.isPatternRooted(2));
        });
    }

    @Test
    void isPatternNonLocal() {
        assertQuery(query -> {
            assertFalse(query.isPatternNonLocal(0));
            assertThrows(IndexOutOfBoundsException.class, () -> query.isPatternNonLocal(2));
        });
    }

    @Test
    void isPatternGuaranteedAtStep() {
        assertQuery(query -> {
            assertFalse(query.isPatternGuaranteedAtStep(27));
            assertThrows(IndexOutOfBoundsException.class, () -> query.isPatternGuaranteedAtStep(99));
        });
    }

    @Test
    void getPatternSettings() {
        assertQuery("((identifier) @foo (#set! foo))", query -> {
            var settings = query.getPatternSettings(0);
            assertTrue(settings.get("foo").isEmpty());
        });
        assertQuery("((identifier) @foo (#set! foo \"FOO\"))", query -> {
            var settings = query.getPatternSettings(0);
            assertEquals("FOO", settings.get("foo").orElse(null));
        });
    }

    @Test
    void getPatternAssertions() {
        assertQuery("((identifier) @foo (#is? foo))", query -> {
            var assertions = query.getPatternAssertions(0, true);
            assertTrue(assertions.get("foo").isEmpty());
        });
        assertQuery("((identifier) @foo (#is-not? foo \"FOO\"))", query -> {
            var assertions = query.getPatternAssertions(0, false);
            assertEquals("FOO", assertions.get("foo").orElse(null));
        });
    }

    @Test
    void queryWithTwoPredicates() {
        var source =
                """
                ((identifier) @foo
                 (#eq? @foo "foo")
                 (#not-eq? @foo "bar"))
                """
                        .stripIndent();
        assertQuery(source, query -> {
            assertEquals(1, query.getPatternCount());
            assertIterableEquals(List.of("foo"), query.getCaptureNames());
            assertIterableEquals(List.of("eq?", "foo", "not-eq?", "bar"), query.getStringValues());
        });
    }
}
