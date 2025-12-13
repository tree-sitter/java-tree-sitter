package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A class that can be used to execute a {@linkplain Query query}
 * on a {@linkplain Tree syntax tree}.
 *
 * @since 0.25.0
 */
@NullMarked
public class QueryCursor implements AutoCloseable {
    private final MemorySegment self;
    private final Arena arena;
    private final Query query;

    /** Create a new cursor for the given query. */
    public QueryCursor(Query query) {
        this.query = query;
        arena = Arena.ofShared();
        self = ts_query_cursor_new().reinterpret(arena, TreeSitter::ts_query_cursor_delete);
    }

    /**
     * Get the maximum number of in-progress matches.
     *
     * @apiNote Defaults to {@code -1} (unlimited).
     */
    public @Unsigned int getMatchLimit() {
        return ts_query_cursor_match_limit(self);
    }

    /**
     * Get the maximum number of in-progress matches.
     *
     * @throws IllegalArgumentException If {@code matchLimit == 0}.
     */
    public QueryCursor setMatchLimit(@Unsigned int matchLimit) throws IllegalArgumentException {
        if (matchLimit == 0) {
            throw new IllegalArgumentException("The match limit cannot equal 0");
        }
        ts_query_cursor_set_match_limit(self, matchLimit);
        return this;
    }

    /**
     * Set the maximum start depth for the query.
     *
     * <p>This prevents cursors from exploring children nodes at a certain depth.
     * <br>Note that if a pattern includes many children, then they will still be checked.
     */
    public QueryCursor setMaxStartDepth(@Unsigned int maxStartDepth) {
        ts_query_cursor_set_max_start_depth(self, maxStartDepth);
        return this;
    }

    /**
     * Set the range of bytes in which the query will be executed.
     *
     * <p>The query cursor will return matches that intersect with the given range.
     * This means that a match may be returned even if some of its captures fall
     * outside the specified range, as long as at least part of the match
     * overlaps with the range.
     *
     * <p>For example, if a query pattern matches a node that spans a larger area
     * than the specified range, but part of that node intersects with the range,
     * the entire match will be returned.
     *
     * @throws IllegalArgumentException If {@code endByte > startByte}.
     */
    public QueryCursor setByteRange(@Unsigned int startByte, @Unsigned int endByte) throws IllegalArgumentException {
        if (!ts_query_cursor_set_byte_range(self, startByte, endByte)) {
            throw new IllegalArgumentException("Invalid byte range");
        }
        return this;
    }

    /**
     * Set the byte range within which all matches must be fully contained.
     *
     * <p>In contrast to {@link #setByteRange(int, int)}, this will restrict the query cursor
     * to only return matches where <em>all</em> nodes are <em>fully</em> contained within
     * the given range. Both functions can be used together, e.g. to search for any matches
     * that intersect line 5000, as long as they are fully contained within lines 4500-5500.
     *
     * @throws IllegalArgumentException If {@code endByte > startByte}.
     * @since 0.26.0
     */
    public QueryCursor setContainingByteRange(@Unsigned int startByte, @Unsigned int endByte)
            throws IllegalArgumentException {
        if (!ts_query_cursor_set_containing_byte_range(self, startByte, endByte)) {
            throw new IllegalArgumentException("Invalid byte range");
        }
        return this;
    }

    /**
     * Set the range of points in which the query will be executed.
     *
     * <p>The query cursor will return matches that intersect with the given range.
     * This means that a match may be returned even if some of its captures fall
     * outside the specified range, as long as at least part of the match
     * overlaps with the range.
     *
     * <p>For example, if a query pattern matches a node that spans a larger area
     * than the specified range, but part of that node intersects with the range,
     * the entire match will be returned.
     *
     * @throws IllegalArgumentException If {@code endPoint > startPoint}.
     */
    public QueryCursor setPointRange(Point startPoint, Point endPoint) throws IllegalArgumentException {
        try (var alloc = Arena.ofConfined()) {
            MemorySegment start = startPoint.into(alloc), end = endPoint.into(alloc);
            if (!ts_query_cursor_set_point_range(self, start, end)) {
                throw new IllegalArgumentException("Invalid point range");
            }
        }
        return this;
    }

    /**
     * Set the point range within which all matches must be fully contained.
     *
     * <p>In contrast to {@link #setPointRange(Point, Point)}, this will restrict the query cursor
     * to only return matches where <em>all</em> nodes are <em>fully</em> contained within
     * the given range. Both functions can be used together, e.g. to search for any matches
     * that intersect line 5000, as long as they are fully contained within lines 4500-5500.
     *
     * @throws IllegalArgumentException If {@code endPoint > startPoint}.
     * @since 0.26.0
     */
    public QueryCursor setContainingPointRange(Point startPoint, Point endPoint) throws IllegalArgumentException {
        try (var alloc = Arena.ofConfined()) {
            MemorySegment start = startPoint.into(alloc), end = endPoint.into(alloc);
            if (!ts_query_cursor_set_containing_point_range(self, start, end)) {
                throw new IllegalArgumentException("Invalid point range");
            }
        }
        return this;
    }

    /**
     * Check if the query exceeded its maximum number of
     * in-progress matches during its last execution.
     */
    public boolean didExceedMatchLimit() {
        return ts_query_cursor_did_exceed_match_limit(self);
    }

    private void exec(Node node, @Nullable Options options) {
        try (var alloc = Arena.ofConfined()) {
            if (options == null || options.progressCallback == null) {
                ts_query_cursor_exec(self, query.segment(), node.copy(alloc));
            } else {
                var cursorOptions = TSQueryCursorOptions.allocate(alloc);
                TSQueryCursorOptions.payload(cursorOptions, MemorySegment.NULL);
                var progress = TSQueryCursorOptions.progress_callback.allocate(
                        (payload) -> {
                            var offset = TSQueryCursorState.current_byte_offset(payload);
                            return options.progressCallback.test(new State(offset));
                        },
                        alloc);
                TSQueryCursorOptions.progress_callback(cursorOptions, progress);
                ts_query_cursor_exec_with_options(self, query.segment(), node.copy(alloc), cursorOptions);
            }
        }
    }

    /**
     * Iterate over all the captures in the order that they were found.
     *
     * <p>This is useful if you don't care about which pattern matched,
     * and just want a single, ordered sequence of captures.
     *
     * @param node The node that the query will run on.
     *
     * @implNote The lifetime of the matches is bound to that of the cursor.
     */
    public Stream<SimpleImmutableEntry<Integer, QueryMatch>> findCaptures(Node node) {
        return findCaptures(node, arena, null);
    }

    /**
     * Iterate over all the captures in the order that they were found.
     *
     * <p>This is useful if you don't care about which pattern matched,
     * and just want a single, ordered sequence of captures.
     *
     * @param node The node that the query will run on.
     * @param options The options of the query cursor.
     *
     * @implNote The lifetime of the matches is bound to that of the cursor.
     */
    public Stream<SimpleImmutableEntry<Integer, QueryMatch>> findCaptures(Node node, Options options) {
        return findCaptures(node, arena, options);
    }

    /**
     * Iterate over all the captures in the order that they were found.
     *
     * <p>This is useful if you don't care about which pattern matched,
     * and just want a single, ordered sequence of captures.
     *
     * @param node The node that the query will run on.
     * @param options The options of the query cursor.
     */
    public Stream<SimpleImmutableEntry<Integer, QueryMatch>> findCaptures(
            Node node, SegmentAllocator allocator, @Nullable Options options) {
        exec(node, options);
        var callback = options != null ? options.predicateCallback : null;
        var iterator = new CapturesIterator(query, self, node.getTree(), allocator, callback);
        return StreamSupport.stream(iterator, false);
    }

    /**
     * Iterate over all the matches in the order that they were found.
     *
     * <p>Because multiple patterns can match the same set of nodes, one match may contain
     * captures that appear <em>before</em> some of the captures from a previous match.
     *
     * @param node The node that the query will run on.
     *
     * @implNote The lifetime of the matches is bound to that of the cursor.
     */
    public Stream<QueryMatch> findMatches(Node node) {
        return findMatches(node, arena, null);
    }

    /**
     * Iterate over all the matches in the order that they were found.
     *
     * <p>Because multiple patterns can match the same set of nodes, one match may contain
     * captures that appear <em>before</em> some of the captures from a previous match.
     *
     * <h4 id="findMatches-example">Predicate Example</h4>
     * <p>
     * {@snippet lang = "java":
     * QueryCursor.Options options = new QueryCursor.Options((predicate, match) -> {
     *     if (!predicate.getName().equals("ieq?")) return true;
     *     List<QueryPredicateArg> args = predicate.getArgs();
     *     Node node = match.findNodes(args.getFirst().value()).getFirst();
     *     return args.getLast().value().equalsIgnoreCase(node.getText());
     * });
     * Stream<QueryMatch> matches = self.findMatches(tree.getRootNode(), options);
     *}
     *
     * @param node The node that the query will run on.
     * @param options The options of the query cursor.
     *
     * @implNote The lifetime of the matches is bound to that of the cursor.
     */
    public Stream<QueryMatch> findMatches(Node node, Options options) {
        return findMatches(node, arena, options);
    }

    /**
     * Iterate over all the matches in the order that they were found, using the given allocator.
     *
     * <p>Because multiple patterns can match the same set of nodes, one match may contain
     * captures that appear <em>before</em> some of the captures from a previous match.
     *
     * @param node The node that the query will run on.
     * @param options The options of the query cursor.
     *
     * @see #findMatches(Node, Options)
     */
    public Stream<QueryMatch> findMatches(Node node, SegmentAllocator allocator, @Nullable Options options) {
        exec(node, options);
        var callback = options != null ? options.predicateCallback : null;
        var iterator = new MatchesIterator(query, self, node.getTree(), allocator, callback);
        return StreamSupport.stream(iterator, false);
    }

    @Override
    public void close() throws RuntimeException {
        arena.close();
    }

    /** A class representing the current state of the query cursor. */
    public static final class State {
        private final @Unsigned int currentByteOffset;

        private State(@Unsigned int currentByteOffset) {
            this.currentByteOffset = currentByteOffset;
        }

        /** Get the current byte offset of the cursor. */
        public @Unsigned int getCurrentByteOffset() {
            return currentByteOffset;
        }

        @Override
        public String toString() {
            return String.format(
                    "QueryCursor.State{currentByteOffset=%s}", Integer.toUnsignedString(currentByteOffset));
        }
    }

    /** A class representing the query cursor options. */
    @NullMarked
    public static class Options {
        private final @Nullable Predicate<State> progressCallback;
        private final @Nullable BiPredicate<QueryPredicate, QueryMatch> predicateCallback;

        /**
         * @since 0.26.0
         */
        public Options() {
            this.progressCallback = null;
            this.predicateCallback = null;
        }

        /**
         * @param progressCallback Progress handler. Return {@code true} to cancel query execution,
         *                         {@code false} to continue query execution.
         * @param predicateCallback Custom predicate handler.
         * @since 0.26.0
         */
        public Options(Predicate<State> progressCallback, BiPredicate<QueryPredicate, QueryMatch> predicateCallback) {
            this.progressCallback = progressCallback;
            this.predicateCallback = predicateCallback;
        }

        /**
         * @param progressCallback Progress handler. Return {@code true} to cancel query execution,
         *                         {@code false} to continue query execution.
         */
        public Options(Predicate<State> progressCallback) {
            this.progressCallback = progressCallback;
            this.predicateCallback = null;
        }

        /**
         * @param predicateCallback Custom predicate handler.
         */
        public Options(BiPredicate<QueryPredicate, QueryMatch> predicateCallback) {
            this.progressCallback = null;
            this.predicateCallback = predicateCallback;
        }
    }
}
