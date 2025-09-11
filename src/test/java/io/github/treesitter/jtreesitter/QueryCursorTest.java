package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class QueryCursorTest {
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

    private static void assertCursor(Consumer<QueryCursor> assertions) {
        assertCursor(source, assertions);
    }

    private static void assertCursor(String source, Consumer<QueryCursor> assertions) {
        try (var query = new Query(language, source)) {
            try (var cursor = new QueryCursor(query)) {
                assertions.accept(cursor);
            }
        }
    }

    @Test
    void getMatchLimit() {
        assertCursor(cursor -> assertEquals(-1, cursor.getMatchLimit()));
    }

    @Test
    void setMatchLimit() {
        assertCursor(cursor -> {
            assertSame(cursor, cursor.setMatchLimit(10));
            assertEquals(10, cursor.getMatchLimit());
        });
    }

    @Test
    void setMaxStartDepth() {
        assertCursor(cursor -> assertSame(cursor, cursor.setMaxStartDepth(10)));
    }

    @Test
    void setByteRange() {
        assertCursor(cursor -> assertSame(cursor, cursor.setByteRange(1, 10)));
    }

    @Test
    void setPointRange() {
        assertCursor(cursor -> {
            Point start = new Point(0, 1), end = new Point(1, 10);
            assertSame(cursor, cursor.setPointRange(start, end));
        });
    }

    @Test
    void didExceedMatchLimit() {
        assertCursor(cursor -> assertFalse(cursor.didExceedMatchLimit()));
    }

    @Test
    void findCaptures() {
        try (var tree = parser.parse("class Foo {}").orElseThrow()) {
            assertCursor(cursor -> {
                var matches = cursor.findCaptures(tree.getRootNode()).toList();
                assertEquals(3, matches.size());
                assertEquals(0, matches.get(0).getKey());
                assertEquals(0, matches.get(1).getKey());
                assertNotEquals(matches.get(0).getValue(), matches.get(1).getValue());
            });
        }
    }

    @Test
    void findMatches() {
        try (var tree = parser.parse("class Foo {}").orElseThrow()) {
            assertCursor(cursor -> {
                var matches = cursor.findMatches(tree.getRootNode()).toList();
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
            assertCursor(source, cursor -> {
                var matches = cursor.findMatches(tree.getRootNode()).toList();
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
            assertCursor(source, cursor -> {
                var matches = cursor.findMatches(tree.getRootNode()).toList();
                assertEquals(1, matches.size());
                assertEquals(
                        "Foo", matches.getFirst().captures().getFirst().node().getText());
            });

            source = """
            ((identifier) @name
             (#not-any-of? @name "Foo" "Bar"))
            """
                    .stripIndent();
            assertCursor(source, cursor -> {
                var matches = cursor.findMatches(tree.getRootNode()).toList();
                assertTrue(matches.isEmpty());
            });

            source = """
                ((identifier) @foo
                 (#ieq? @foo "foo"))
                """
                    .stripIndent();
            assertCursor(source, cursor -> {
                var options = new QueryCursor.Options((predicate, match) -> {
                    if (!predicate.getName().equals("ieq?")) return true;
                    var args = predicate.getArgs();
                    var node = match.findNodes(args.getFirst().value()).getFirst();
                    return args.getLast().value().equalsIgnoreCase(node.getText());
                });
                var matches = cursor.findMatches(tree.getRootNode(), options).toList();
                assertEquals(1, matches.size());
                assertEquals(
                        "Foo", matches.getFirst().captures().getFirst().node().getText());
            });
        }

        // Verify that capture count is treated as `uint16_t` and not as signed Java `short`
        try (var tree = parser.parse(";".repeat(Short.MAX_VALUE + 1)).orElseThrow()) {
            var source = """
                ";"+ @capture
                """;
            assertCursor(source, cursor -> {
                var matches = cursor.findMatches(tree.getRootNode()).toList();
                assertEquals(1, matches.size());
                assertEquals(Short.MAX_VALUE + 1, matches.getFirst().captures().size());
            });
        }

        // Verify that `eq?` predicate works with quantified captures
        try (var tree = parser.parse("/* 1 */ /* 1 */ /* 1 */").orElseThrow()) {
            var source = """
                (program
                  . (block_comment) @b (block_comment)+ @a
                  (#eq? @a @b)
                )
                """;
            assertCursor(source, cursor -> {
                var matches = cursor.findMatches(tree.getRootNode()).toList();
                assertEquals(1, matches.size());
                assertEquals(
                    "/* 1 */", matches.getFirst().captures().getFirst().node().getText());
            });
        }
    }
}
