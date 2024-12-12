package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import org.junit.jupiter.api.*;

import java.lang.foreign.Arena;

class TreeCursorTest {
    private static Tree tree;
    private TreeCursor cursor;

    @BeforeAll
    static void beforeAll() {
        var language = new Language(TreeSitterJava.language());
        try (var parser = new Parser(language)) {
            tree = parser.parse("class Foo {}").orElseThrow();
        }
    }

    @AfterAll
    static void afterAll() {
        tree.close();
    }

    @BeforeEach
    void setUp() {
        cursor = tree.walk();
    }

    @AfterEach
    void tearDown() {
        if(cursor != null){
            cursor.close();
        }
    }

    @Test
    void getCurrentNode() {
        var node = cursor.getCurrentNode();
        assertEquals(tree.getRootNode(), node);
        assertSame(node, cursor.getCurrentNode());
    }

    @Test
    void getCurrentNodeWithCustomAllocator() {

        try(var arena = Arena.ofConfined()){
            var node = cursor.getCurrentNode(arena);
            assertEquals(tree.getRootNode(), node);
            cursor.close();
            cursor = null; // avoid double close
            // can still access node after cursor was closed
            assertEquals(tree.getRootNode(), node);
        }

    }

    @Test
    void getCurrentDepth() {
        assertEquals(0, cursor.getCurrentDepth());
    }

    @Test
    void getCurrentFieldId() {
        assertEquals(0, cursor.getCurrentFieldId());
    }

    @Test
    void getCurrentFieldName() {
        assertNull(cursor.getCurrentFieldName());
    }

    @Test
    void getCurrentDescendantIndex() {
        assertEquals(0, cursor.getCurrentDescendantIndex());
    }

    @Test
    void gotoFirstChild() {
        assertTrue(cursor.gotoFirstChild());
        assertEquals(1, cursor.getCurrentDepth());
    }

    @Test
    void gotoLastChild() {
        assertTrue(cursor.gotoLastChild());
        assertEquals(1, cursor.getCurrentDescendantIndex());
    }

    @Test
    void gotoParent() {
        assertFalse(cursor.gotoParent());
    }

    @Test
    void gotoNextSibling() {
        assertTrue(cursor.gotoFirstChild());
        assertFalse(cursor.gotoNextSibling());
    }

    @Test
    void gotoPreviousSibling() {
        assertTrue(cursor.gotoLastChild());
        assertFalse(cursor.gotoPreviousSibling());
    }

    @Test
    void gotoDescendant() {
        cursor.gotoDescendant(2);
        assertEquals(2, cursor.getCurrentDescendantIndex());
        assertEquals("class", cursor.getCurrentNode().getText());
    }

    @Test
    void gotoFirstChildForByte() {
        assertEquals(0, cursor.gotoFirstChildForByte(1).orElseThrow());
        assertEquals("class_declaration", cursor.getCurrentNode().getType());
        assertTrue(cursor.gotoFirstChildForByte(13).isEmpty());
    }

    @Test
    void gotoFirstChildForPoint() {
        assertTrue(cursor.gotoFirstChild());
        assertEquals(1, cursor.gotoFirstChildForPoint(new Point(0, 7)).orElseThrow());
        assertEquals("name", cursor.getCurrentFieldName());
        assertTrue(cursor.gotoFirstChildForPoint(new Point(1, 0)).isEmpty());
    }

    @Test
    @DisplayName("reset(node)")
    void resetNode() {
        var root = tree.getRootNode();
        assertTrue(cursor.gotoFirstChild());
        assertNotEquals(root, cursor.getCurrentNode());
        cursor.reset(root);
        assertEquals(root, cursor.getCurrentNode());
    }

    @Test
    @DisplayName("reset(cursor)")
    void resetCursor() {
        var copy = cursor.clone();
        var root = tree.getRootNode();
        assertTrue(cursor.gotoFirstChild());
        assertNotEquals(root, cursor.getCurrentNode());
        cursor.reset(copy);
        assertEquals(root, cursor.getCurrentNode());
    }

    @Test
    @DisplayName("clone()")
    void copy() {
        var copy = cursor.clone();
        assertNotSame(cursor, copy);
        assertTrue(copy.gotoFirstChild());
        assertNotEquals(cursor.getCurrentNode(), copy.getCurrentNode());
    }
}
