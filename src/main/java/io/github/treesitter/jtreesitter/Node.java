package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.TSNode;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A single node within a {@linkplain Tree syntax tree}.
 *
 * @implNote Node lifetimes are tied to the {@link Tree},
 * {@link TreeCursor}, or {@link Query} that they belong to.
 */
@NullMarked
public final class Node {
    private final MemorySegment self;
    private final Tree tree;
    private @Nullable List<Node> children;
    private final Arena arena = Arena.ofAuto();

    Node(MemorySegment self, Tree tree) {
        this.self = self;
        this.tree = tree;
    }

    private Optional<Node> optional(MemorySegment node) {
        return ts_node_is_null(node) ? Optional.empty() : Optional.of(new Node(node, tree));
    }

    MemorySegment copy(Arena arena) {
        return self.reinterpret(arena, null);
    }

    /** Get the tree that contains this node. */
    public Tree getTree() {
        return tree;
    }

    /**
     * Get the numerical ID of the node.
     *
     * @apiNote Within any given syntax tree, no two nodes have the same ID. However,
     * if a new tree is created based on an  older tree, and a node from the old tree
     * is reused in the process, then  that node will have the same ID in both trees.
     */
    public @Unsigned long getId() {
        return TSNode.id(self).address();
    }

    /** Get the numerical ID of the node's type. */
    public @Unsigned short getSymbol() {
        return ts_node_symbol(self);
    }

    /** Get the numerical ID of the node's type, as it appears in the grammar ignoring aliases. */
    public @Unsigned short getGrammarSymbol() {
        return ts_node_grammar_symbol(self);
    }

    /** Get the type of the node. */
    public String getType() {
        return ts_node_type(self).getString(0);
    }

    /** Get the type of the node, as it appears in the grammar ignoring aliases. */
    public String getGrammarType() {
        return ts_node_grammar_type(self).getString(0);
    }

    /**
     * Check if the node is <em>named</em>.
     *
     * <p>Named nodes correspond to named rules in the grammar,
     * whereas <em>anonymous</em> nodes correspond to string literals.
     */
    public boolean isNamed() {
        return ts_node_is_named(self);
    }

    /**
     * Check if the node is <em>extra</em>.
     *
     * <p>Extra nodes represent things which are not required
     * by the grammar but can appear anywhere (e.g. whitespace).
     */
    public boolean isExtra() {
        return ts_node_is_extra(self);
    }

    /** Check if the node is an {@index ERROR} node. */
    public boolean isError() {
        return ts_node_is_error(self);
    }

    /**
     * Check if the node is {@index MISSING}.
     *
     * <p>MISSING nodes are inserted by the parser in order
     * to recover from certain kinds of syntax errors.
     */
    public boolean isMissing() {
        return ts_node_is_missing(self);
    }

    /** Check if the node has been edited. */
    public boolean hasChanges() {
        return ts_node_has_changes(self);
    }

    /**
     * Check if the node is an {@index ERROR},
     * or contains any {@index ERROR} nodes.
     */
    public boolean hasError() {
        return ts_node_has_error(self);
    }

    /** Get the parse state of this node. */
    public @Unsigned short getParseState() {
        return ts_node_parse_state(self);
    }

    /** Get the parse state after this node. */
    public @Unsigned short getNextParseState() {
        return ts_node_next_parse_state(self);
    }

    /** Get the start byte of the node. */
    public @Unsigned int getStartByte() {
        return ts_node_start_byte(self);
    }

    /** Get the end byte of the node. */
    public @Unsigned int getEndByte() {
        return ts_node_end_byte(self);
    }

    /** Get the range of the node. */
    public Range getRange() {
        return new Range(getStartPoint(), getEndPoint(), getStartByte(), getEndByte());
    }

    /** Get the start point of the node. */
    public Point getStartPoint() {
        return Point.from(ts_node_start_point(arena, self));
    }

    /** Get the end point of the node. */
    public Point getEndPoint() {
        return Point.from(ts_node_end_point(arena, self));
    }

    /** Get the number of this node's children. */
    public @Unsigned int getChildCount() {
        return ts_node_child_count(self);
    }

    /** Get the number of this node's <em>named</em> children. */
    public @Unsigned int getNamedChildCount() {
        return ts_node_named_child_count(self);
    }

    /** Get the number of this node's descendants, including the node itself. */
    public @Unsigned int getDescendantCount() {
        return ts_node_descendant_count(self);
    }

    /** The node's immediate parent, if any. */
    public Optional<Node> getParent() {
        return optional(ts_node_parent(arena, self));
    }

    /** The node's next sibling, if any. */
    public Optional<Node> getNextSibling() {
        return optional(ts_node_next_sibling(arena, self));
    }

    /** The node's previous sibling, if any. */
    public Optional<Node> getPrevSibling() {
        return optional(ts_node_prev_sibling(arena, self));
    }

    /** The node's next <em>named</em> sibling, if any. */
    public Optional<Node> getNextNamedSibling() {
        return optional(ts_node_next_named_sibling(arena, self));
    }

    /** The node's previous <em>named</em> sibling, if any. */
    public Optional<Node> getPrevNamedSibling() {
        return optional(ts_node_prev_named_sibling(arena, self));
    }

    /**
     * Get the node's child at the given index, if any.
     *
     * @apiNote This method is fairly fast, but its cost is technically
     * {@code log(i)}, so if you might be iterating over a long list of children,
     * you should use {@link #getChildren()} or {@link #walk()} instead.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getChildCount() child count}.
     */
    public Optional<Node> getChild(@Unsigned int index) throws IndexOutOfBoundsException {
        if (index >= getChildCount()) {
            throw new IndexOutOfBoundsException(
                    "Child index %s is out of bounds".formatted(Integer.toUnsignedString(index)));
        }
        return optional(ts_node_child(arena, self, index));
    }

    /**
     * Get the node's <em>named</em> child at the given index, if any.
     *
     * @apiNote This method is fairly fast, but its cost is technically
     * {@code log(i)}, so if you might be iterating over a long list of children,
     * you should use {@link #getNamedChildren()} or {@link #walk()} instead.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getNamedChildCount() child count}.
     */
    public Optional<Node> getNamedChild(@Unsigned int index) throws IndexOutOfBoundsException {
        if (index >= getNamedChildCount()) {
            throw new IndexOutOfBoundsException(
                    "Child index %s is out of bounds".formatted(Integer.toUnsignedString(index)));
        }
        return optional(ts_node_named_child(arena, self, index));
    }

    /**
     * Get the node's first child with the given field ID, if any.
     *
     * @see Language#getFieldIdForName
     */
    public Optional<Node> getChildByFieldId(@Unsigned short id) {
        return optional(ts_node_child_by_field_id(arena, self, id));
    }

    /** Get the node's first child with the given field name, if any. */
    public Optional<Node> getChildByFieldName(String name) {
        var segment = arena.allocateFrom(name);
        return optional(ts_node_child_by_field_name(arena, self, segment, name.length()));
    }

    /**
     * Get this node's children.
     *
     * @apiNote If you're walking the tree recursively, you may want to use {@link #walk()} instead.
     */
    public List<Node> getChildren() {
        if (this.children == null) {
            var length = getChildCount();
            if (length == 0) return Collections.emptyList();
            var children = new ArrayList<Node>(length);
            var cursor = ts_tree_cursor_new(arena, self);
            ts_tree_cursor_goto_first_child(cursor);
            for (int i = 0; i < length; ++i) {
                var node = ts_tree_cursor_current_node(arena, cursor);
                children.add(new Node(node, tree));
                ts_tree_cursor_goto_next_sibling(cursor);
            }
            ts_tree_cursor_delete(cursor);
            this.children = Collections.unmodifiableList(children);
        }
        return this.children;
    }

    /** Get this node's <em>named</em> children. */
    public List<Node> getNamedChildren() {
        return getChildren().stream().filter(Node::isNamed).toList();
    }

    /**
     * Get a list of the node's children with the given field ID.
     *
     * @see Language#getFieldIdForName
     */
    public List<Node> getChildrenByFieldId(@Unsigned short id) {
        if (id == 0) return Collections.emptyList();
        var length = getChildCount();
        var children = new ArrayList<Node>(length);
        var cursor = ts_tree_cursor_new(arena, self);
        var ok = ts_tree_cursor_goto_first_child(cursor);
        while (ok) {
            if (ts_tree_cursor_current_field_id(cursor) == id) {
                var node = ts_tree_cursor_current_node(arena, cursor);
                children.add(new Node(node, tree));
            }
            ok = ts_tree_cursor_goto_next_sibling(cursor);
        }
        ts_tree_cursor_delete(cursor);
        children.trimToSize();
        return children;
    }

    /** Get a list of the node's child with the given field name. */
    public List<Node> getChildrenByFieldName(String name) {
        return getChildrenByFieldId(tree.getLanguage().getFieldIdForName(name));
    }

    /**
     * Get the field name of this node’s child at the given index, if available.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getNamedChildCount() child count}.
     */
    public @Nullable String getFieldNameForChild(@Unsigned int index) throws IndexOutOfBoundsException {
        if (index >= getChildCount()) {
            throw new IndexOutOfBoundsException(
                    "Child index %s is out of bounds".formatted(Integer.toUnsignedString(index)));
        }
        var segment = ts_node_field_name_for_child(self, index);
        return segment.equals(MemorySegment.NULL) ? null : segment.getString(0);
    }

    /**
     * Get the field name of this node's <em>named</em> child at the given index, if available.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getNamedChildCount() child count}.
     * @since 0.24.0
     */
    public @Nullable String getFieldNameForNamedChild(@Unsigned int index) throws IndexOutOfBoundsException {
        if (index >= getChildCount()) {
            throw new IndexOutOfBoundsException(
                    "Child index %s is out of bounds".formatted(Integer.toUnsignedString(index)));
        }
        var segment = ts_node_field_name_for_named_child(self, index);
        return segment.equals(MemorySegment.NULL) ? null : segment.getString(0);
    }

    /**
     * Get the smallest node within this node that spans the given byte range, if any.
     *
     * @throws IllegalArgumentException If {@code start > end}.
     */
    public Optional<Node> getDescendant(@Unsigned int start, @Unsigned int end) throws IllegalArgumentException {
        if (Integer.compareUnsigned(start, end) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Start byte %s exceeds end byte %s",
                    Integer.toUnsignedString(start), Integer.toUnsignedString(end)));
        }
        return optional(ts_node_descendant_for_byte_range(arena, self, start, end));
    }

    /**
     * Get the smallest node within this node that spans the given point range, if any.
     *
     * @throws IllegalArgumentException If {@code start > end}.
     */
    public Optional<Node> getDescendant(Point start, Point end) throws IllegalArgumentException {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("Start point %s exceeds end point %s".formatted(start, end));
        }
        MemorySegment startPoint = start.into(arena), endPoint = end.into(arena);
        return optional(ts_node_descendant_for_point_range(arena, self, startPoint, endPoint));
    }

    /**
     * Get the smallest <em>named</em> node within this node that spans the given byte range, if any.
     *
     * @throws IllegalArgumentException If {@code start > end}.
     */
    public Optional<Node> getNamedDescendant(@Unsigned int start, @Unsigned int end) throws IllegalArgumentException {
        if (Integer.compareUnsigned(start, end) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Start byte %s exceeds end byte %s",
                    Integer.toUnsignedString(start), Integer.toUnsignedString(end)));
        }
        return optional(ts_node_named_descendant_for_byte_range(arena, self, start, end));
    }

    /**
     * Get the smallest <em>named</em> node within this node that spans the given point range, if any.
     *
     * @throws IllegalArgumentException If {@code start > end}.
     */
    public Optional<Node> getNamedDescendant(Point start, Point end) {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("Start point %s exceeds end point %s".formatted(start, end));
        }
        MemorySegment startPoint = start.into(arena), endPoint = end.into(arena);
        return optional(ts_node_named_descendant_for_point_range(arena, self, startPoint, endPoint));
    }

    /** Get the child of the node that contains the given descendant, if any. */
    public Optional<Node> getChildContainingDescendant(Node descendant) {
        return optional(ts_node_child_containing_descendant(arena, self, descendant.self));
    }

    /** Get the source code of the node, if available. */
    public @Nullable String getText() {
        var text = tree.getText();
        if (text == null) return null;
        var endByte = Math.min(getEndByte(), text.length());
        return text.substring(getStartByte(), endByte);
    }

    /**
     * Edit this node to keep it in-sync with source code that has been edited.
     *
     * @apiNote This method is only rarely needed. When you edit a syntax
     * tree via {@link Tree#edit}, all of the nodes that you retrieve from
     * the tree afterward will already reflect the edit. You only need
     * to use this when you have a specific {@linkplain Node} instance
     * that you want to keep and continue to use after an edit.
     */
    public void edit(InputEdit edit) {
        ts_node_edit(self, edit.into(arena));
        children = null;
    }

    /** Create a new tree cursor starting from this node. */
    public TreeCursor walk() {
        return new TreeCursor(this, tree);
    }

    /** Get the S-expression representing the node. */
    public String toSexp() {
        var string = ts_node_string(self);
        var result = string.getString(0);
        free(string);
        return result;
    }

    /** Check if two nodes are identical. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node other)) return false;
        return ts_node_eq(self, other.self);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(getId());
    }

    @Override
    public String toString() {
        return String.format(
                "Node{type=%s, startByte=%s, endByte=%s}",
                getType(), Integer.toUnsignedString(getStartByte()), Integer.toUnsignedString(getEndByte()));
    }
}
