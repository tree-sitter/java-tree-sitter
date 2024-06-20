package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import java.util.List;
import org.junit.jupiter.api.*;

class TreeTest {
    private static final String source = "class Foo {}";
    private static Language language;
    private static Parser parser;
    private Tree tree;

    @BeforeAll
    static void beforeAll() {
        language = new Language(TreeSitterJava.language());
        parser = new Parser(language);
    }

    @AfterAll
    static void afterAll() {
        parser.close();
    }

    @BeforeEach
    void setUp() {
        tree = parser.parse(source).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        tree.close();
    }

    @Test
    void getLanguage() {
        assertSame(language, tree.getLanguage());
    }

    @Test
    void getText() {
        assertEquals(source, tree.getText());
    }

    @Test
    void getRootNode() {
        assertEquals("program", tree.getRootNode().getType());
    }

    @Test
    void getRootNodeWithOffset() {
        var node = tree.getRootNodeWithOffset(6, new Point(0, 6));
        assertNotNull(node);
        assertEquals(source.substring(6), node.getText());
    }

    @Test
    void getIncludedRanges() {
        assertIterableEquals(List.of(Range.DEFAULT), tree.getIncludedRanges());
    }

    @Test
    void getChangedRanges() {
        tree.edit(new InputEdit(0, 0, 7, new Point(0, 0), new Point(0, 0), new Point(0, 7)));
        var newSource = "public %s".formatted(source);
        try (var newTree = parser.parse(newSource, tree).orElseThrow()) {
            var range = tree.getChangedRanges(newTree).getFirst();
            assertEquals(7, range.endByte());
            assertEquals(7, range.endPoint().column());
        }
    }

    @Test
    void edit() {
        tree.edit(new InputEdit(9, 9, 10, new Point(0, 10), new Point(0, 9), new Point(0, 10)));
        assertNull(tree.getText());
    }

    @Test
    void walk() {
        try (var cursor = tree.walk()) {
            assertEquals(tree.getRootNode(), cursor.getCurrentNode());
        }
    }

    @Test
    @DisplayName("clone()")
    void copy() {
        try (var copy = tree.clone()) {
            assertNotSame(tree, copy);
        }
    }
}
