package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.OptionalInt;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** A class that can be used to efficiently walk a {@linkplain Tree syntax tree}. */
@NullMarked
public final class TreeCursor implements AutoCloseable, Cloneable {
    private final MemorySegment self;
    private final Arena arena;
    private final Tree tree;
    private @Nullable Node node;

    TreeCursor(Node node, Tree tree) {
        arena = Arena.ofShared();
        self = ts_tree_cursor_new(arena, node.copy(arena));
        this.tree = tree;
    }

    TreeCursor(Tree tree) {
        arena = Arena.ofShared();
        var node = ts_tree_root_node(arena, tree.segment());
        self = ts_tree_cursor_new(arena, node);
        this.tree = tree;
    }

    private TreeCursor(TreeCursor cursor) {
        arena = Arena.ofShared();
        self = ts_tree_cursor_copy(arena, cursor.self);
        tree = cursor.tree.clone();
        node = cursor.node;
    }

    /** Get the current node of the cursor. */
    public Node getCurrentNode() {
        if (this.node == null) {
            var node = ts_tree_cursor_current_node(arena, self);
            this.node = new Node(node, tree);
        }
        return this.node;
    }

    /**
     * Get the depth of the cursor's current node relative to
     * the original node that the cursor was constructed with.
     */
    public @Unsigned int getCurrentDepth() {
        return ts_tree_cursor_current_depth(self);
    }

    /**
     * Get the field ID of the tree cursor's current node, or {@code 0}.
     *
     * @see Node#getChildByFieldId
     * @see Language#getFieldIdForName
     */
    public @Unsigned short getCurrentFieldId() {
        return ts_tree_cursor_current_field_id(self);
    }

    /**
     * Get the field name of the tree cursor's current node, or {@code null}.
     *
     * @see Node#getChildByFieldName
     */
    public @Nullable String getCurrentFieldName() {
        var segment = ts_tree_cursor_current_field_name(self);
        return segment.equals(MemorySegment.NULL) ? null : segment.getString(0);
    }

    /**
     * Get the index of the cursor's current node out of the descendants
     * of the original node that the cursor was constructed with.
     */
    public @Unsigned int getCurrentDescendantIndex() {
        return ts_tree_cursor_current_descendant_index(self);
    }

    /**
     * Move the cursor to the first child of its current node.
     *
     * @return {@code true} if the cursor successfully moved, or
     *         {@code false} if there were no children.
     */
    public boolean gotoFirstChild() {
        var result = ts_tree_cursor_goto_first_child(self);
        if (result) node = null;
        return result;
    }

    /**
     * Move the cursor to the last child of its current node.
     *
     * @return {@code true} if the cursor successfully moved, or
     *         {@code false} if there were no children.
     */
    public boolean gotoLastChild() {
        var result = ts_tree_cursor_goto_last_child(self);
        if (result) node = null;
        return result;
    }

    /**
     * Move the cursor to the parent of its current node.
     *
     * @return {@code true} if the cursor successfully moved, or
     *         {@code false} if there was no parent node.
     */
    public boolean gotoParent() {
        var result = ts_tree_cursor_goto_parent(self);
        if (result) node = null;
        return result;
    }

    /**
     * Move the cursor to the next sibling of its current node.
     *
     * @return {@code true} if the cursor successfully moved, or
     *         {@code false} if there was no next sibling node.
     */
    public boolean gotoNextSibling() {
        var result = ts_tree_cursor_goto_next_sibling(self);
        if (result) node = null;
        return result;
    }

    /**
     * Move the cursor to the previous sibling of its current node.
     *
     * @return {@code true} if the cursor successfully moved, or
     *         {@code false} if there was no previous sibling node.
     */
    public boolean gotoPreviousSibling() {
        var result = ts_tree_cursor_goto_previous_sibling(self);
        if (result) node = null;
        return result;
    }

    /**
     * Move the cursor to the node that is the nth descendant of
     * the original node that the cursor was constructed with.
     *
     * @apiNote The index {@code 0} represents the original node itself.
     */
    public void gotoDescendant(@Unsigned int index) {
        ts_tree_cursor_goto_descendant(self, index);
        node = null;
    }

    /**
     * Move the cursor to the first child of its current
     * node that extends beyond the given byte offset.
     *
     * @return The index of the child node, if found.
     */
    public @Unsigned OptionalInt gotoFirstChildForByte(@Unsigned int offset) {
        var index = ts_tree_cursor_goto_first_child_for_byte(self, offset);
        if (index == -1L) return OptionalInt.empty();
        node = null;
        return OptionalInt.of((int) index);
    }

    /**
     * Move the cursor to the first child of its current
     * node that extends beyond the given point.
     *
     * @return The index of the child node, if found.
     */
    public @Unsigned OptionalInt gotoFirstChildForPoint(Point point) {
        try (var arena = Arena.ofConfined()) {
            var goal = point.into(arena);
            var index = ts_tree_cursor_goto_first_child_for_point(self, goal);
            if (index == -1L) return OptionalInt.empty();
            node = null;
            return OptionalInt.of((int) index);
        }
    }

    /** Reset the cursor to start at a different node. */
    public void reset(Node node) {
        try (var arena = Arena.ofConfined()) {
            ts_tree_cursor_reset(self, node.copy(arena));
        } finally {
            this.node = null;
        }
    }

    /** Reset the cursor to start at the same position as another cursor. */
    public void reset(TreeCursor cursor) {
        ts_tree_cursor_reset_to(self, cursor.self);
        this.node = null;
    }

    /** Create a shallow copy of the tree cursor. */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public TreeCursor clone() {
        return new TreeCursor(this);
    }

    @Override
    public void close() throws RuntimeException {
        ts_tree_cursor_delete(self);
        arena.close();
    }

    @Override
    public String toString() {
        return "TreeCursor{tree=%s}".formatted(tree);
    }
}
