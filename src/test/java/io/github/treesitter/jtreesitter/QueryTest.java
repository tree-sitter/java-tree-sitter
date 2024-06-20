package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
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
        // NOTE: can't use _ because of palantir/palantir-java-format#934
        try (var q = new Query(language, source)) {
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
    void getCaptureCount() {
        assertQuery(query -> assertEquals(3, query.getCaptureCount()));
    }

    @Test
    void getMatchLimit() {
        assertQuery(query -> assertEquals(-1, query.getMatchLimit()));
    }

    @Test
    void setMatchLimit() {
        assertQuery(query -> {
            assertSame(query, query.setMatchLimit(10));
            assertEquals(10, query.getMatchLimit());
        });
    }

    @Test
    void setMaxStartDepth() {
        assertQuery(query -> assertSame(query, query.setMaxStartDepth(10)));
    }

    @Test
    void setByteRange() {
        assertQuery(query -> assertSame(query, query.setByteRange(1, 10)));
    }

    @Test
    void setPointRange() {
        assertQuery(query -> {
            Point start = new Point(0, 1), end = new Point(1, 10);
            assertSame(query, query.setPointRange(start, end));
        });
    }

    @Test
    void didExceedMatchLimit() {
        assertQuery(query -> assertFalse(query.didExceedMatchLimit()));
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
    void findMatches() {
        try (var tree = parser.parse("class Foo {}").orElseThrow()) {
            assertQuery(query -> {
                var matches = query.findMatches(tree.getRootNode()).toList();
                assertEquals(2, matches.size());
                assertEquals(0, matches.getFirst().patternIndex());
                assertEquals(1, matches.getLast().patternIndex());
            });
        }

        try (var tree = parser.parse("int y = x + 1;").orElseThrow()) {
            var source =
                    """
                ((variable_declarator
                  (identifier) @y
                  (binary_expression
                    (identifier) @x))
                  (#not-eq? @y @x))
                """
                            .stripIndent();
            assertQuery(source, query -> {
                var matches = query.findMatches(tree.getRootNode()).toList();
                assertEquals(1, matches.size());
                assertEquals(
                        "y", matches.getFirst().captures().getFirst().node().getText());
            });
        }

        try (var tree = parser.parse("class Foo{}\nclass Bar {}").orElseThrow()) {
            var source = """
                ((identifier) @foo
                 (#eq? @foo "Foo"))
                """
                    .stripIndent();
            assertQuery(source, query -> {
                var matches = query.findMatches(tree.getRootNode()).toList();
                assertEquals(1, matches.size());
                assertEquals(
                        "Foo", matches.getFirst().captures().getFirst().node().getText());
            });

            source =
                    """
                ((identifier) @name
                 (#not-any-of? @name "Foo" "Bar"))
                """
                            .stripIndent();
            assertQuery(source, query -> {
                var matches = query.findMatches(tree.getRootNode()).toList();
                assertTrue(matches.isEmpty());
            });

            source = """
                ((identifier) @foo
                 (#ieq? @foo "foo"))
                """
                    .stripIndent();
            assertQuery(source, query -> {
                var matches = query.findMatches(tree.getRootNode(), (predicate, match) -> {
                            if (!predicate.getName().equals("ieq?")) return true;
                            var args = predicate.getArgs();
                            var node = match.findNodes(args.getFirst().value()).getFirst();
                            return args.getLast().value().equalsIgnoreCase(node.getText());
                        })
                        .toList();
                assertEquals(1, matches.size());
                assertEquals(
                        "Foo", matches.getFirst().captures().getFirst().node().getText());
            });
        }
    }
}
