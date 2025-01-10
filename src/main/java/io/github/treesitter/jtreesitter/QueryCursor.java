package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.TSNode;
import io.github.treesitter.jtreesitter.internal.TSQueryCapture;
import io.github.treesitter.jtreesitter.internal.TSQueryMatch;
import io.github.treesitter.jtreesitter.internal.TreeSitter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Cursor for iterating over the matches produced by a {@link Query}.
 * <p>
 * An instance of this class can be retrieved by calling {@link Query#execute(Node)}.
 */
@NullMarked
public class QueryCursor implements AutoCloseable {

    private final MemorySegment cursor;
    private final Arena arena;

    private final Query query;
    private final Tree tree;

    QueryCursor(Query query, Node cursorRootNode, @Nullable QueryCursorConfig config) {
        arena = Arena.ofConfined();
        cursor = ts_query_cursor_new().reinterpret(arena, TreeSitter::ts_query_cursor_delete);
        this.query = query;
        this.tree = cursorRootNode.getTree();
        if (config != null) {
            applyConfig(config);
        }

        try (var alloc = Arena.ofConfined()) {
            ts_query_cursor_exec(cursor, query.self(), cursorRootNode.copy(alloc));
        }
    }

    private void applyConfig(QueryCursorConfig options) {

        if (options.getStartByte() >= 0 && options.getEndByte() >= 0) {
            ts_query_cursor_set_byte_range(cursor, options.getStartByte(), options.getEndByte());
        }

        if (options.getStartPoint() != null && options.getEndPoint() != null) {
            try (var alloc = Arena.ofConfined()) {
                MemorySegment start = options.getStartPoint().into(alloc);
                MemorySegment end = options.getEndPoint().into(alloc);
                ts_query_cursor_set_point_range(cursor, start, end);
            }
        }

        if (options.getMaxStartDepth() >= 0) {
            ts_query_cursor_set_max_start_depth(cursor, options.getMaxStartDepth());
        }

        if (options.getMatchLimit() >= 0) {
            ts_query_cursor_set_match_limit(cursor, options.getMatchLimit());
        }

        if (options.getTimeoutMicros() >= 0) {
            ts_query_cursor_set_timeout_micros(cursor, options.getTimeoutMicros());
        }
    }

    /**
     * Get the maximum number of in-progress matches.
     *
     * @apiNote Defaults to {@code -1} (unlimited).
     */
    public @Unsigned int getMatchLimit() {
        return ts_query_cursor_match_limit(cursor);
    }

    /**
     * Get the maximum duration in microseconds that query
     * execution should be allowed to take before halting.
     *
     * @apiNote Defaults to {@code 0} (unlimited).
     * @since 0.23.1
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public @Unsigned long getTimeoutMicros() {
        return ts_query_cursor_timeout_micros(cursor);
    }

    /**
     * Check if the query exceeded its maximum number of
     * in-progress matches during its last execution.
     */
    public boolean didExceedMatchLimit() {
        return ts_query_cursor_did_exceed_match_limit(cursor);
    }

    /**
     * Stream the matches produced by the query. The stream can not be consumed after the cursor is closed. The native
     * nodes backing the matches are bound to the lifetime of the cursor.
     * @return a stream of matches
     */
    public Stream<QueryMatch> matchStream() {
        return matchStream(null);
    }

    /**
     * Like {@link #matchStream()} but allows for custom predicates to be applied to the matches.
     * @param predicate a function to handle custom predicates.
     * @return a stream of matches
     */
    public Stream<QueryMatch> matchStream(@Nullable BiPredicate<QueryPredicate, QueryMatch> predicate) {
        return matchStream(arena, predicate);
    }

    /**
     * Like {@link #matchStream(BiPredicate)} but allows for a custom allocator to be used for allocating the native nodes.
     * @param allocator allocator to use for allocating the native nodes backing the matches
     * @param predicate a function to handle custom predicates.
     * @return a stream of matches
     */
    public Stream<QueryMatch> matchStream(
            SegmentAllocator allocator, @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate) {
        return StreamSupport.stream(new MatchesIterator(this, allocator, predicate), false);
    }

    /**
     * Get the next match produced by the query. The native nodes backing the match are bound to the lifetime of the cursor.
     * @return the next match, if available
     */
    public Optional<QueryMatch> nextMatch() {
        return nextMatch(null);
    }

    /**
     * Like {@link #nextMatch()} but allows for custom predicates to be applied to the matches.
     * @param predicate a function to handle custom predicates.
     * @return the next match, if available
     */
    public Optional<QueryMatch> nextMatch(@Nullable BiPredicate<QueryPredicate, QueryMatch> predicate) {
        return nextMatch(arena, predicate);
    }

    /**
     * Like {@link #nextMatch(BiPredicate)} but allows for a custom allocator to be used for allocating the native nodes.
     * @param allocator allocator to use for allocating the native nodes backing the matches
     * @param predicate a function to handle custom predicates.
     * @return the next match, if available
     */
    public Optional<QueryMatch> nextMatch(
            SegmentAllocator allocator, @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate) {

        var hasNoText = tree.getText() == null;
        MemorySegment match = arena.allocate(TSQueryMatch.layout());
        while (ts_query_cursor_next_match(cursor, match)) {
            var count = Short.toUnsignedInt(TSQueryMatch.capture_count(match));
            var matchCaptures = TSQueryMatch.captures(match);
            var captureList = new ArrayList<QueryCapture>(count);
            for (int i = 0; i < count; ++i) {
                var capture = TSQueryCapture.asSlice(matchCaptures, i);
                var name = query.getCaptureNames().get(TSQueryCapture.index(capture));
                var node = TSNode.allocate(allocator).copyFrom(TSQueryCapture.node(capture));
                captureList.add(new QueryCapture(name, new Node(node, tree)));
            }
            var patternIndex = TSQueryMatch.pattern_index(match);
            var result = new QueryMatch(patternIndex, captureList);
            if (hasNoText || matches(predicate, result)) {
                return Optional.of(result);
            }
        }
        return Optional.empty();
    }

    private boolean matches(@Nullable BiPredicate<QueryPredicate, QueryMatch> predicate, QueryMatch match) {
        return query.getPredicates().get(match.patternIndex()).stream().allMatch(p -> {
            if (p.getClass() != QueryPredicate.class) return p.test(match);
            return predicate == null || predicate.test(p, match);
        });
    }

    @Override
    public void close() {
        arena.close();
    }

    private static final class MatchesIterator extends Spliterators.AbstractSpliterator<QueryMatch> {

        private final @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate;
        private final SegmentAllocator allocator;

        private final QueryCursor cursor;

        public MatchesIterator(
                QueryCursor cursor,
                SegmentAllocator allocator,
                @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate) {
            super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL);
            this.predicate = predicate;
            this.allocator = allocator;
            this.cursor = cursor;
        }

        @Override
        public boolean tryAdvance(Consumer<? super QueryMatch> action) {

            if (!cursor.arena.scope().isAlive()) {
                throw new IllegalStateException("The underlying QueryCursor is closed. Cannot produce more matches.");
            }

            Optional<QueryMatch> queryMatch = cursor.nextMatch(allocator, predicate);
            queryMatch.ifPresent(action);
            return queryMatch.isPresent();
        }
    }
}
