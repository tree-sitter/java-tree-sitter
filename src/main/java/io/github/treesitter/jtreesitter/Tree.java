package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.TSRange;
import io.github.treesitter.jtreesitter.internal.TreeSitter;
import java.lang.foreign.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** A class that represents a syntax tree. */
@NullMarked
public final class Tree implements AutoCloseable, Cloneable {
    private final MemorySegment self;
    private @Nullable String source;
    private final Arena arena;
    private final Language language;
    private @Nullable List<Range> includedRanges;

    Tree(MemorySegment self, Language language, @Nullable String source) {
        arena = Arena.ofShared();
        this.self = self.reinterpret(arena, TreeSitter::ts_tree_delete);
        this.language = language;
        this.source = source;
    }

    private Tree(Tree tree) {
        var copy = ts_tree_copy(tree.self);
        arena = Arena.ofShared();
        self = copy.reinterpret(arena, TreeSitter::ts_tree_delete);
        language = tree.language;
        source = tree.source;
        includedRanges = tree.includedRanges;
    }

    MemorySegment segment() {
        return self;
    }

    /** Get the language that was used to parse the syntax tree. */
    public Language getLanguage() {
        return language;
    }

    /** Get the source code of the syntax tree, if available. */
    public @Nullable String getText() {
        return source;
    }

    /** Get the root node of the syntax tree. */
    public Node getRootNode() {
        return new Node(ts_tree_root_node(arena, self), this);
    }

    /**
     * Get the root node of the syntax tree, but with
     * its position shifted forward by the given offset.
     */
    public @Nullable Node getRootNodeWithOffset(@Unsigned int bytes, Point extent) {
        try (var alloc = Arena.ofShared()) {
            var offsetExtent = extent.into(alloc);
            var node = ts_tree_root_node_with_offset(arena, self, bytes, offsetExtent);
            if (ts_node_is_null(node)) return null;
            return new Node(node, this);
        }
    }

    /** Get the included ranges of the syntax tree. */
    public List<Range> getIncludedRanges() {
        if (includedRanges == null) {
            try (var alloc = Arena.ofConfined()) {
                var length = alloc.allocate(C_INT.byteSize(), C_INT.byteAlignment());
                var ranges = ts_tree_included_ranges(self, length);
                int size = length.get(C_INT, 0);
                if (size == 0) return Collections.emptyList();

                includedRanges = new ArrayList<>(size);
                for (int i = 0; i < size; ++i) {
                    var range = TSRange.asSlice(ranges, i);
                    includedRanges.add(Range.from(range));
                }
                free(ranges);
            }
        }
        return Collections.unmodifiableList(includedRanges);
    }

    /**
     * Compare an old edited syntax tree to a new
     * syntax tree representing the same document.
     *
     * <p>For this to work correctly, this tree must have been
     * edited such that its ranges match up to the new tree.
     *
     * @return A list of ranges whose syntactic structure has changed.
     */
    public List<Range> getChangedRanges(Tree newTree) {
        try (var alloc = Arena.ofConfined()) {
            var length = alloc.allocate(C_INT.byteSize(), C_INT.byteAlignment());
            var ranges = ts_tree_get_changed_ranges(self, newTree.self, length);
            int size = length.get(C_INT, 0);
            if (size == 0) return Collections.emptyList();

            var changedRanges = new ArrayList<Range>(size);
            for (int i = 0; i < size; ++i) {
                var range = TSRange.asSlice(ranges, i);
                changedRanges.add(Range.from(range));
            }
            free(ranges);
            return changedRanges;
        }
    }

    /**
     * Edit the syntax tree to keep it in sync
     * with source code that has been modified.
     */
    public void edit(InputEdit edit) {
        try (var alloc = Arena.ofConfined()) {
            ts_tree_edit(self, edit.into(alloc));
        } finally {
            source = null;
        }
    }

    /** Create a new tree cursor starting from the root node of the tree. */
    public TreeCursor walk() {
        return new TreeCursor(this);
    }

    /**
     * Create a shallow copy of the syntax tree.
     *
     * @implNote You need to clone a tree in order to use it on more than
     * one thread at a time, as {@linkplain Tree} objects are not thread safe.
     */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Tree clone() {
        return new Tree(this);
    }

    @Override
    public void close() throws RuntimeException {
        arena.close();
    }

    @Override
    public String toString() {
        return "Tree{language=%s, source=%s}".formatted(language, source);
    }
}
