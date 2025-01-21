package io.github.treesitter.jtreesitter;

import static org.junit.jupiter.api.Assertions.*;

import io.github.treesitter.jtreesitter.languages.TreeSitterJava;
import org.junit.jupiter.api.*;

class NodeTest {
    private static Tree tree;
    private static Node node;

    @BeforeAll
    static void beforeAll() {
        var language = new Language(TreeSitterJava.language());
        try (var parser = new Parser(language)) {
            tree = parser.parse("class Foo {} // uni©ode").orElseThrow();
            node = tree.getRootNode();
        }
    }

    @AfterAll
    static void afterAll() {
        tree.close();
    }

    @Test
    void getTree() {
        assertSame(tree, node.getTree());
    }

    @Test
    void getId() {
        assertNotEquals(0L, node.getId());
    }

    @Test
    void getSymbol() {
        assertEquals(138, node.getSymbol());
    }

    @Test
    void getGrammarSymbol() {
        assertEquals(138, node.getGrammarSymbol());
    }

    @Test
    void getType() {
        assertEquals("program", node.getType());
    }

    @Test
    void getGrammarType() {
        assertEquals("program", node.getGrammarType());
    }

    @Test
    void isNamed() {
        assertTrue(node.isNamed());
    }

    @Test
    void isExtra() {
        assertFalse(node.isExtra());
    }

    @Test
    void isError() {
        assertFalse(node.isError());
    }

    @Test
    void isMissing() {
        assertFalse(node.isMissing());
    }

    @Test
    void hasChanges() {
        assertFalse(node.hasChanges());
    }

    @Test
    void hasError() {
        assertFalse(node.hasError());
    }

    @Test
    void getParseState() {
        assertEquals(0, node.getParseState());
    }

    @Test
    void getNextParseState() {
        assertEquals(0, node.getNextParseState());
    }

    @Test
    void getStartByte() {
        assertEquals(0, node.getStartByte());
    }

    @Test
    void getEndByte() {
        assertEquals(24, node.getEndByte());
    }

    @Test
    void getRange() {
        Point startPoint = new Point(0, 0), endPoint = new Point(0, 24);
        assertEquals(new Range(startPoint, endPoint, 0, 24), node.getRange());
    }

    @Test
    void getStartPoint() {
        assertEquals(new Point(0, 0), node.getStartPoint());
    }

    @Test
    void getEndPoint() {
        assertEquals(new Point(0, 24), node.getEndPoint());
    }

    @Test
    void getChildCount() {
        assertEquals(2, node.getChildCount());
    }

    @Test
    void getNamedChildCount() {
        assertEquals(2, node.getNamedChildCount());
    }

    @Test
    void getDescendantCount() {
        assertEquals(8, node.getDescendantCount());
    }

    @Test
    void getParent() {
        assertTrue(node.getParent().isEmpty());
    }

    @Test
    void getNextSibling() {
        assertTrue(node.getNextSibling().isEmpty());
    }

    @Test
    void getPrevSibling() {
        assertTrue(node.getPrevSibling().isEmpty());
    }

    @Test
    void getNextNamedSibling() {
        assertTrue(node.getNextNamedSibling().isEmpty());
    }

    @Test
    void getPrevNamedSibling() {
        assertTrue(node.getPrevNamedSibling().isEmpty());
    }

    @Test
    void getChild() {
        var child = node.getChild(0).orElseThrow();
        assertEquals("class_declaration", child.getType());
    }

    @Test
    void getNamedChild() {
        var child = node.getNamedChild(0).orElseThrow();
        assertEquals("class_declaration", child.getGrammarType());
    }

    @Test
    void getFirstChildForByte() {
        var child = node.getFirstChildForByte(15).orElseThrow();
        assertEquals("line_comment", child.getGrammarType());
    }

    @Test
    void getFirstNamedChildForByte() {
        var child = node.getFirstNamedChildForByte(15).orElseThrow();
        assertEquals("line_comment", child.getGrammarType());
    }

    @Test
    void getChildByFieldId() {
        var child = node.getChild(0).orElseThrow();
        child = child.getChildByFieldId((short) 20).orElseThrow();
        assertEquals("identifier", child.getType());
    }

    @Test
    void getChildByFieldName() {
        var child = node.getChild(0).orElseThrow();
        child = child.getChildByFieldName("name").orElseThrow();
        assertEquals("identifier", child.getGrammarType());
    }

    @Test
    void getChildren() {
        var children = node.getChild(0).orElseThrow().getChildren();
        assertEquals(3, children.size());
        assertEquals("class", children.getFirst().getType());
    }

    @Test
    void getNamedChildren() {
        var children = node.getChild(0).orElseThrow().getNamedChildren();
        assertEquals(2, children.size());
        assertEquals("identifier", children.getFirst().getType());
    }

    @Test
    void getChildrenByFieldId() {
        var children = node.getChild(0).orElseThrow().getChildrenByFieldId((short) 1);
        assertTrue(children.isEmpty());
    }

    @Test
    void getChildrenByFieldName() {
        var children = node.getChild(0).orElseThrow().getChildrenByFieldName("body");
        assertEquals(1, children.size());
        assertEquals("class_body", children.getFirst().getType());
    }

    @Test
    void getFieldNameForChild() {
        var child = node.getChild(0).orElseThrow();
        assertNull(child.getFieldNameForChild(0));
        assertEquals("body", child.getFieldNameForChild(2));
    }

    @Test
    void getFieldNameForNamedChild() {
        var child = node.getChild(0).orElseThrow();
        assertNull(child.getFieldNameForNamedChild(2));
    }

    @Test
    @DisplayName("getDescendant(bytes)")
    void getDescendantBytes() {
        var descendant = node.getDescendant(0, 5).orElseThrow();
        assertEquals("class", descendant.getType());
    }

    @Test
    @DisplayName("getDescendant(points)")
    void getDescendantPoints() {
        Point startPoint = new Point(0, 10), endPoint = new Point(0, 12);
        var descendant = node.getDescendant(startPoint, endPoint).orElseThrow();
        assertEquals("class_body", descendant.getGrammarType());
    }

    @Test
    @DisplayName("getNamedDescendant(bytes)")
    void getNamedDescendantBytes() {
        var descendant = node.getNamedDescendant(0, 5).orElseThrow();
        assertEquals("class_declaration", descendant.getType());
    }

    @Test
    @DisplayName("getNamedDescendant(points)")
    void getNamedDescendantPoints() {
        Point startPoint = new Point(0, 6), endPoint = new Point(0, 9);
        var descendant = node.getNamedDescendant(startPoint, endPoint).orElseThrow();
        assertEquals("identifier", descendant.getGrammarType());
    }

    @Test
    @SuppressWarnings("removal")
    void getChildContainingDescendant() {
        var descendant = node.getChild(0).orElseThrow();
        descendant = descendant.getChild(0).orElseThrow();
        var child = node.getChildContainingDescendant(descendant);
        assertEquals("class_declaration", child.orElseThrow().getType());
    }

    @Test
    void getChildWithDescendant() {
        var descendant = node.getChild(0).orElseThrow();
        var child = node.getChildWithDescendant(descendant);
        assertEquals("class_declaration", child.orElseThrow().getType());
    }

    @Test
    void getText() {
        var child = node.getChild(1).orElseThrow();
        assertEquals("// uni©ode", child.getText());
    }

    @Test
    void edit() {
        var edit = new InputEdit(0, 12, 10, new Point(0, 0), new Point(0, 12), new Point(0, 10));
        try (var copy = tree.clone()) {
            var node = copy.getRootNode();
            copy.edit(edit);
            node.edit(edit);
            assertTrue(node.hasChanges());
        }
    }

    @Test
    void walk() {
        var child = node.getChild(0).orElseThrow();
        try (var cursor = child.walk()) {
            assertEquals(child, cursor.getCurrentNode());
        }
    }

    @Test
    void toSexp() {
        var sexp = "(program (class_declaration name: (identifier) body: (class_body)) (line_comment))";
        assertEquals(sexp, node.toSexp());
    }

    @Test
    void equals() {
        var other = node.getChild(0).orElseThrow();
        assertNotEquals(node, other);
        other = other.getParent().orElseThrow();
        assertEquals(node, other);
    }
}
