package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import java.lang.foreign.Arena;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class QueryCursorTest {

    private static Language language;
    private static Parser parser;

    @BeforeAll
    static void beforeAll() {
        language = new Language(TreeSitterJava.language());
        parser = new Parser(language);
    }

    @AfterAll
    static void afterAll() {
        parser.close();
    }

    private static void assertQueryCursor(String querySource, String source, Consumer<QueryCursor> assertions) {
        assertQueryCursor(querySource, source, null, assertions);
    }

    private static void assertQueryCursor(
            String querySource, String source, QueryCursorConfig config, Consumer<QueryCursor> assertions) {
        try (var tree = parser.parse(source).orElseThrow();
                Query query = new Query(language, querySource)) {
            assertQueryCursor(query, tree.getRootNode(), config, assertions);
        } catch (QueryError e) {
            fail("Unexpected query error", e);
        }
    }

    private static void assertQueryCursor(
            Query query, Node node, QueryCursorConfig config, Consumer<QueryCursor> assertions) {
        try (var queryCursor = query.execute(node, config)) {
            assertions.accept(queryCursor);
        }
    }

    @Test
    void nextMatch() {
        String querySource = "(identifier) @identifier";

        String source = """
            int a = b;
            """;

        assertQueryCursor(querySource, source, qc -> {
            Optional<QueryMatch> currentMatch = qc.nextMatch();

            assertTrue(currentMatch.isPresent());
            assertEquals("a", currentMatch.get().captures().getFirst().node().getText());

            currentMatch = qc.nextMatch();
            assertTrue(currentMatch.isPresent());
            assertEquals("b", currentMatch.get().captures().getFirst().node().getText());

            currentMatch = qc.nextMatch();
            assertFalse(currentMatch.isPresent());
        });
    }

    @Test
    void matchStream() {

        String querySource = "(identifier) @identifier";

        String source = """
            int a = b;
            int c = 3;
            """;

        assertQueryCursor(querySource, source, qc -> {
            List<QueryMatch> queryMatches = qc.matchStream().toList();
            assertEquals(3, queryMatches.size());

            List<String> texts = queryMatches.stream()
                    .flatMap(qm -> qm.captures().stream())
                    .map(cap -> cap.node().getText())
                    .toList();
            assertEquals(List.of("a", "b", "c"), texts);
        });
    }

    @Test
    void didExceedMatchLimit() {
        String querySource = "(identifier) @identifier";

        String source = """
            int a = b;
            int c = 3;
            """;

        assertQueryCursor(querySource, source, qc -> {
            assertFalse(qc.didExceedMatchLimit());
        });
    }

    @Test
    void testByteRange() {

        String querySource = "(identifier) @identifier";

        String source = """
            int a = b;
            int c = 3;
            """;

        QueryCursorConfig config = new QueryCursorConfig();
        config.setByteRange(6, 20); // should ignore a

        assertQueryCursor(querySource, source, config, qc -> {
            List<QueryMatch> queryMatches = qc.matchStream().toList();
            assertEquals(2, queryMatches.size());

            List<String> texts = queryMatches.stream()
                    .flatMap(qm -> qm.captures().stream())
                    .map(cap -> cap.node().getText())
                    .toList();
            assertEquals(List.of("b", "c"), texts);
        });
    }

    @Test
    void testPointRange() {
        String querySource = "(identifier) @identifier";

        String source = """
            int a = b;
            int c = 3;
            """;

        QueryCursorConfig config = new QueryCursorConfig();
        config.setPointRange(new Point(0, 0), new Point(1, 0)); // should ignore c

        assertQueryCursor(querySource, source, config, qc -> {
            List<QueryMatch> queryMatches = qc.matchStream().toList();
            assertEquals(2, queryMatches.size());

            List<String> texts = queryMatches.stream()
                    .flatMap(qm -> qm.captures().stream())
                    .map(cap -> cap.node().getText())
                    .toList();
            assertEquals(List.of("a", "b"), texts);
        });
    }

    @Test
    void testStartDepth() {

        String queryString = "(local_variable_declaration) @decl";

        String source =
                """
            int a = b;
            void foo() {
                int c = 3;
            }
            """;

        QueryCursorConfig config = new QueryCursorConfig();
        config.setMaxStartDepth(1); // should ignore second declaration as it is nested in method body

        assertQueryCursor(queryString, source, config, qc -> {
            List<QueryMatch> queryMatches = qc.matchStream().toList();
            assertEquals(1, queryMatches.size());

            List<String> texts = queryMatches.stream()
                    .flatMap(qm -> qm.captures().stream())
                    .map(cap -> cap.node().getText())
                    .toList();
            assertEquals(List.of("int a = b;"), texts);
        });
    }

    @Test
    void testCustomAllocator() {
        String querySource = "(identifier) @identifier";

        String source = """
            int a = b;
            int c = 3;
            """;

        try (Arena arena = Arena.ofConfined()) {

            List<QueryMatch> matches = new ArrayList<>();

            try (var tree = parser.parse(source).orElseThrow();
                    Query query = new Query(language, querySource)) {

                try (var queryCursor = query.execute(tree.getRootNode())) {
                    List<QueryMatch> queryMatches =
                            queryCursor.matchStream(arena, null).toList();
                    matches.addAll(queryMatches);
                }

                // check that we can still work with the nodes after closing the cursor
                List<String> parentTexts = matches.stream()
                        .flatMap(qm -> qm.captures().stream())
                        .flatMap(cap -> cap.node().getParent().stream())
                        .map(Node::getText)
                        .toList();

                assertEquals(List.of("a = b", "a = b", "c = 3"), parentTexts);
            } catch (QueryError e) {
                fail("Unexpected query error", e);
            }
        }
    }

    @Test
    void testUseMatchStreamAfterClose() {
        String querySource = "(identifier) @identifier";

        String source = """
            int a = b;
            int c = 3;
            """;

        try (var tree = parser.parse(source).orElseThrow();
                Query query = new Query(language, querySource)) {

            Stream<QueryMatch> queryStream = null;
            try (var queryCursor = query.execute(tree.getRootNode())) {
                queryStream = queryCursor.matchStream();
            }
            // we cannot use the stream after closing the cursor
            assertThrows(IllegalStateException.class, queryStream::toList);
        } catch (QueryError e) {
            fail("Unexpected query error", e);
        }
    }
}
