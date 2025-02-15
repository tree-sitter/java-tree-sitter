package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.C_INT;
import static io.github.treesitter.jtreesitter.internal.TreeSitter.ts_query_cursor_next_capture;

import io.github.treesitter.jtreesitter.internal.TSQueryMatch;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class CapturesIterator extends Spliterators.AbstractSpliterator<SimpleImmutableEntry<Integer, QueryMatch>> {
    private final @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate;
    private final Tree tree;
    private final SegmentAllocator allocator;
    private final Query query;
    private final MemorySegment cursor;

    public CapturesIterator(
            Query query,
            MemorySegment cursor,
            Tree tree,
            SegmentAllocator allocator,
            @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate) {
        super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL);
        this.predicate = predicate;
        this.tree = tree;
        this.allocator = allocator;
        this.query = query;
        this.cursor = cursor;
    }

    @Override
    public boolean tryAdvance(Consumer<? super SimpleImmutableEntry<Integer, QueryMatch>> action) {
        var hasNoText = tree.getText() == null;
        MemorySegment match = allocator.allocate(TSQueryMatch.layout());
        MemorySegment index = allocator.allocate(C_INT);
        var captureNames = query.getCaptureNames();
        while (ts_query_cursor_next_capture(cursor, match, index)) {
            var result = QueryMatch.from(match, captureNames, tree, allocator);
            if (hasNoText || query.matches(predicate, result)) {
                var entry = new SimpleImmutableEntry<>(index.get(C_INT, 0), result);
                action.accept(entry);
                return true;
            }
        }
        return false;
    }
}
