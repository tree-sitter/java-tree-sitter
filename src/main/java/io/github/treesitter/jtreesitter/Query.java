package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A class that represents a set of patterns which match
 * {@linkplain Node nodes} in a {@linkplain Tree syntax tree}.
 *
 * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers#query-syntax">Query Syntax</a>
 */
@NullMarked
public final class Query implements AutoCloseable {
    private final MemorySegment query;
    private final MemorySegment cursor;
    private final Arena arena;
    private final Language language;
    private final String source;
    private final List<String> captureNames;
    private final List<List<QueryPredicate>> predicates;
    private final List<Map<String, Optional<String>>> settings;
    private final List<Map<String, Optional<String>>> positiveAssertions;
    private final List<Map<String, Optional<String>>> negativeAssertions;

    Query(Language language, String source) throws QueryError {
        arena = Arena.ofShared();
        var string = arena.allocateFrom(source);
        var errorOffset = arena.allocate(C_INT);
        var errorType = arena.allocate(C_INT);
        var query = ts_query_new(language.segment(), string, source.length(), errorOffset, errorType);
        if (query.equals(MemorySegment.NULL)) {
            long start = 0, row = 0;
            int offset = errorOffset.get(C_INT, 0);
            for (var line : source.split("\n")) {
                long end = start + line.length() + 1;
                if (end > offset) break;
                start = end;
                row += 1;
            }
            long column = offset - start, type = errorType.get(C_INT, 0);
            if (type == TSQueryErrorSyntax()) {
                if (offset >= source.length()) throw new QueryError.Syntax();
                throw new QueryError.Syntax(row, column);
            } else if (type == TSQueryErrorCapture()) {
                int index = 0, length = source.length();
                var suffix = source.subSequence(offset, length);
                for (; index < length; ++index) {
                    if (invalidPredicateChar(suffix.charAt(index))) break;
                }
                throw new QueryError.Capture(row, column, suffix.subSequence(0, index));
            } else if (type == TSQueryErrorNodeType()) {
                int index = 0, length = source.length();
                var suffix = source.subSequence(offset, length);
                for (; index < length; ++index) {
                    if (invalidIdentifierChar(suffix.charAt(index))) break;
                }
                throw new QueryError.NodeType(row, column, suffix.subSequence(0, index));
            } else if (type == TSQueryErrorField()) {
                int index = 0, length = source.length();
                var suffix = source.subSequence(offset, length);
                for (; index < length; ++index) {
                    if (invalidIdentifierChar(suffix.charAt(index))) break;
                }
                throw new QueryError.Field(row, column, suffix.subSequence(0, index));
            } else if (type == TSQueryErrorStructure()) {
                throw new QueryError.Structure(row, column);
            } else {
                throw new IllegalStateException("Unexpected query error");
            }
        }

        this.language = language;
        this.source = source;
        this.query = query.reinterpret(arena, TreeSitter::ts_query_delete);
        cursor = ts_query_cursor_new().reinterpret(arena, TreeSitter::ts_query_cursor_delete);

        var captureCount = ts_query_capture_count(this.query);
        captureNames = new ArrayList<>(captureCount);
        try (var alloc = Arena.ofConfined()) {
            for (int i = 0; i < captureCount; ++i) {
                var length = alloc.allocate(C_INT);
                var name = ts_query_capture_name_for_id(query, i, length);
                if (length.get(C_INT, 0) == 0) {
                    throw new IllegalStateException("Failed to get capture name at index %d".formatted(i));
                }
                captureNames.add(name.getString(0));
            }
        }

        var patternCount = ts_query_pattern_count(this.query);
        predicates = generate(ArrayList::new, patternCount);
        settings = generate(HashMap::new, patternCount);
        positiveAssertions = generate(HashMap::new, patternCount);
        negativeAssertions = generate(HashMap::new, patternCount);

        var stringCount = ts_query_string_count(this.query);
        List<String> stringValues = new ArrayList<>(stringCount);
        try (var alloc = Arena.ofConfined()) {
            for (int i = 0; i < stringCount; ++i) {
                var length = alloc.allocate(C_INT);
                var name = ts_query_string_value_for_id(query, i, length);
                if (length.get(C_INT, 0) == 0) {
                    throw new IllegalStateException("Failed to get string value at index %d".formatted(i));
                }
                stringValues.add(name.getString(0));
            }
        }

        try (var alloc = Arena.ofConfined()) {
            for (int i = 0, steps; i < patternCount; ++i) {
                var count = alloc.allocate(C_INT);
                var tokens = ts_query_predicates_for_pattern(query, i, count);
                if ((steps = count.get(C_INT, 0)) == 0) continue;
                int offset = ts_query_start_byte_for_pattern(query, i);
                long row = source.chars().limit(offset).filter(c -> c == '\n').count();
                for (long j = 0, nargs = 0; j < steps; ++j) {
                    for (; ; ++nargs) {
                        var t = TSQueryPredicateStep.asSlice(tokens, nargs);
                        if (TSQueryPredicateStep.type(t) == TSQueryPredicateStepTypeDone()) break;
                    }
                    var t0 = TSQueryPredicateStep.asSlice(tokens, 0);
                    if (TSQueryPredicateStep.type(t0) == TSQueryPredicateStepTypeCapture()) {
                        var name = captureNames.get(TSQueryPredicateStep.value_id(t0));
                        throw new QueryError.Predicate(row, "@%s".formatted(name));
                    }
                    var predicate = stringValues.get(TSQueryPredicateStep.value_id(t0));
                    if (QueryPredicate.Eq.NAMES.contains(predicate)) {
                        if (nargs != 3) {
                            var error = "#%s expects 2 arguments, got %d";
                            throw new QueryError.Predicate(row, error, predicate, nargs - 1);
                        }
                        var t1 = TSQueryPredicateStep.asSlice(tokens, 1);
                        if (TSQueryPredicateStep.type(t1) != TSQueryPredicateStepTypeCapture()) {
                            var value = stringValues.get(TSQueryPredicateStep.value_id(t1));
                            var error = "first argument to #%s must be a capture name, got \"%s\"";
                            throw new QueryError.Predicate(row, error, predicate, value);
                        }
                        var capture = captureNames.get(TSQueryPredicateStep.value_id(t1));
                        var t2 = TSQueryPredicateStep.asSlice(tokens, 2);
                        var id = TSQueryPredicateStep.value_id(t2);
                        var isCapture = TSQueryPredicateStep.type(t2) == TSQueryPredicateStepTypeCapture();
                        var value = isCapture ? captureNames.get(id) : stringValues.get(id);
                        predicates.get(i).add(new QueryPredicate.Eq(predicate, capture, value, isCapture));
                    } else if (QueryPredicate.Match.NAMES.contains(predicate)) {
                        if (nargs != 3) {
                            var error = "#%s expects 2 arguments, got %d";
                            throw new QueryError.Predicate(row, error, predicate, nargs - 1);
                        }
                        var t1 = TSQueryPredicateStep.asSlice(tokens, 1);
                        if (TSQueryPredicateStep.type(t1) != TSQueryPredicateStepTypeCapture()) {
                            var value = stringValues.get(TSQueryPredicateStep.value_id(t1));
                            var error = "first argument to #%s must be a capture name, got \"%s\"";
                            throw new QueryError.Predicate(row, error, predicate, value);
                        }
                        var t2 = TSQueryPredicateStep.asSlice(tokens, 2);
                        if (TSQueryPredicateStep.type(t2) != TSQueryPredicateStepTypeString()) {
                            var value = captureNames.get(TSQueryPredicateStep.value_id(t2));
                            var error = "second argument to #%s must be a string literal, got @%s";
                            throw new QueryError.Predicate(row, error, predicate, value);
                        }
                        try {
                            var capture = captureNames.get(TSQueryPredicateStep.value_id(t1));
                            var pattern = Pattern.compile(stringValues.get(TSQueryPredicateStep.value_id(t2)));
                            predicates.get(i).add(new QueryPredicate.Match(predicate, capture, pattern));
                        } catch (PatternSyntaxException e) {
                            throw new QueryError.Predicate(row, "pattern error", e);
                        }
                    } else if (QueryPredicate.AnyOf.NAMES.contains(predicate)) {
                        if (nargs < 3) {
                            var error = "#%s expects at least 2 arguments, got %d";
                            throw new QueryError.Predicate(row, error, predicate, nargs - 1);
                        }
                        var t1 = TSQueryPredicateStep.asSlice(tokens, 1);
                        if (TSQueryPredicateStep.type(t1) != TSQueryPredicateStepTypeCapture()) {
                            var value = stringValues.get(TSQueryPredicateStep.value_id(t1));
                            var error = "first argument to #%s must be a capture name, got \"%s\"";
                            throw new QueryError.Predicate(row, error, predicate, value);
                        }
                        List<String> values = new ArrayList<>((int) nargs - 2);
                        for (long k = 2; k < nargs; ++k) {
                            var t = TSQueryPredicateStep.asSlice(tokens, k);
                            if (TSQueryPredicateStep.type(t) != TSQueryPredicateStepTypeString()) {
                                var value = captureNames.get(TSQueryPredicateStep.value_id(t));
                                var error = "arguments to #%s must be string literals, got @%s";
                                throw new QueryError.Predicate(row, error, predicate, value);
                            }
                            values.add(stringValues.get(TSQueryPredicateStep.value_id(t)));
                        }
                        var capture = captureNames.get(TSQueryPredicateStep.value_id(t1));
                        predicates.get(i).add(new QueryPredicate.AnyOf(predicate, capture, values));
                    } else if (predicate.equals("is?") || predicate.equals("is-not?") || predicate.equals("set!")) {
                        if (nargs == 1 || nargs > 3) {
                            var error = "#%s expects 1-2 arguments, got %d";
                            throw new QueryError.Predicate(row, error, predicate, nargs - 1);
                        }
                        var t1 = TSQueryPredicateStep.asSlice(tokens, 1);
                        if (TSQueryPredicateStep.type(t1) != TSQueryPredicateStepTypeString()) {
                            var value = captureNames.get(TSQueryPredicateStep.value_id(t1));
                            var error = "first argument to #%s must be a string literal, got @%s";
                            throw new QueryError.Predicate(row, error, predicate, value);
                        }
                        String key = stringValues.get(TSQueryPredicateStep.value_id(t1)), value = null;
                        if (nargs == 3) {
                            var t2 = TSQueryPredicateStep.asSlice(tokens, 2);
                            if (TSQueryPredicateStep.type(t2) != TSQueryPredicateStepTypeString()) {
                                var capture = captureNames.get(TSQueryPredicateStep.value_id(t2));
                                var error = "second argument to #%s must be a string literal, got @%s";
                                throw new QueryError.Predicate(row, error, predicate, capture);
                            }
                            value = stringValues.get(TSQueryPredicateStep.value_id(t2));
                        }
                        if (predicate.equals("is?")) {
                            positiveAssertions.get(i).put(key, Optional.ofNullable(value));
                        } else if (predicate.equals("is-not?")) {
                            negativeAssertions.get(i).put(key, Optional.ofNullable(value));
                        } else {
                            settings.get(i).put(key, Optional.ofNullable(value));
                        }
                    } else {
                        List<QueryPredicateArg> values = new ArrayList<>((int) nargs - 1);
                        for (long k = 1; k < nargs; ++k) {
                            var t = TSQueryPredicateStep.asSlice(tokens, k);
                            if (TSQueryPredicateStep.type(t) == TSQueryPredicateStepTypeString()) {
                                var value = stringValues.get(TSQueryPredicateStep.value_id(t));
                                values.add(new QueryPredicateArg.Literal(value));
                            } else {
                                var capture = captureNames.get(TSQueryPredicateStep.value_id(t));
                                values.add(new QueryPredicateArg.Capture(capture));
                            }
                        }
                        predicates.get(i).add(new QueryPredicate(predicate, values));
                    }
                    j += nargs;
                    tokens = TSQueryPredicateStep.asSlice(tokens, nargs);
                }
            }
        }
    }

    private static <T> List<T> generate(Supplier<T> supplier, int limit) {
        return Stream.generate(supplier).limit(limit).toList();
    }

    private static boolean invalidIdentifierChar(char c) {
        return !Character.isLetterOrDigit(c) && c != '_';
    }

    private static boolean invalidPredicateChar(char c) {
        return !(Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '?' || c == '!');
    }

    /** Get the number of patterns in the query. */
    public @Unsigned int getPatternCount() {
        return ts_query_pattern_count(query);
    }

    /** Get the number of captures in the query. */
    public @Unsigned int getCaptureCount() {
        return ts_query_capture_count(query);
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
     * Get the maximum number of in-progress matches.
     *
     * @throws IllegalArgumentException If {@code matchLimit == 0}.
     */
    public Query setMatchLimit(@Unsigned int matchLimit) throws IllegalArgumentException {
        if (matchLimit == 0) {
            throw new IllegalArgumentException("The match limit cannot equal 0");
        }
        ts_query_cursor_set_match_limit(cursor, matchLimit);
        return this;
    }

    /**
     * Get the maximum duration in microseconds that query
     * execution should be allowed to take before halting.
     *
     * @apiNote Defaults to {@code 0} (unlimited).
     * @since 0.23.1
     */
    public @Unsigned long getTimeoutMicros() {
        return ts_query_cursor_timeout_micros(cursor);
    }

    /**
     * Set the maximum duration in microseconds that query
     * execution should be allowed to take before halting.
     *
     * @since 0.23.1
     */
    public Query setTimeoutMicros(@Unsigned long timeoutMicros) {
        ts_query_cursor_set_timeout_micros(cursor, timeoutMicros);
        return this;
    }

    /**
     * Set the maximum start depth for the query.
     *
     * <p>This prevents cursors from exploring children nodes at a certain depth.
     * <br>Note that if a pattern includes many children, then they will still be checked.
     */
    public Query setMaxStartDepth(@Unsigned int maxStartDepth) {
        ts_query_cursor_set_max_start_depth(cursor, maxStartDepth);
        return this;
    }

    /** Set the range of bytes in which the query will be executed. */
    public Query setByteRange(@Unsigned int startByte, @Unsigned int endByte) {
        ts_query_cursor_set_byte_range(cursor, startByte, endByte);
        return this;
    }

    /** Set the range of points in which the query will be executed. */
    public Query setPointRange(Point startPoint, Point endPoint) {
        try (var alloc = Arena.ofConfined()) {
            MemorySegment start = startPoint.into(alloc), end = endPoint.into(alloc);
            ts_query_cursor_set_point_range(cursor, start, end);
        }
        return this;
    }

    /**
     * Check if the query exceeded its maximum number of
     * in-progress matches during its last execution.
     */
    public boolean didExceedMatchLimit() {
        return ts_query_cursor_did_exceed_match_limit(cursor);
    }

    /**
     * Disable a certain pattern within a query.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getPatternCount pattern count}.
     * @apiNote This prevents the pattern from matching and removes most of the overhead
     *          associated with the pattern. Currently, there is no way to undo this.
     */
    public void disablePattern(@Unsigned int index) throws IndexOutOfBoundsException {
        checkIndex(index);
        ts_query_disable_pattern(query, index);
    }

    /**
     * Disable a certain capture within a query.
     *
     * @throws NoSuchElementException If the capture does not exist.
     * @apiNote This prevents the capture from being returned in matches,
     *          and also avoids most resource usage  associated with recording
     *          the capture. Currently, there is no way to undo this.
     */
    public void disableCapture(String name) throws NoSuchElementException {
        if (!captureNames.remove(name)) {
            throw new NoSuchElementException("Capture @%s does not exist".formatted(name));
        }
        try (var alloc = Arena.ofConfined()) {
            ts_query_disable_capture(query, alloc.allocateFrom(name), name.length());
        }
    }

    /**
     * Get the byte offset where the given pattern starts in the query's source.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getPatternCount pattern count}.
     */
    public @Unsigned int startByteForPattern(@Unsigned int index) throws IndexOutOfBoundsException {
        checkIndex(index);
        return ts_query_start_byte_for_pattern(query, index);
    }

    /**
     * Get the byte offset where the given pattern ends in the query's source.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getPatternCount pattern count}.
     * @since 0.23.0
     */
    public @Unsigned int endByteForPattern(@Unsigned int index) throws IndexOutOfBoundsException {
        checkIndex(index);
        return ts_query_end_byte_for_pattern(query, index);
    }

    /**
     * Check if the pattern with the given index has a single root node.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getPatternCount pattern count}.
     */
    public boolean isPatternRooted(@Unsigned int index) throws IndexOutOfBoundsException {
        checkIndex(index);
        return ts_query_is_pattern_rooted(query, index);
    }

    /**
     * Check if the pattern with the given index is "non-local".
     *
     * <p>A non-local pattern has multiple root nodes and can match within
     * a repeating sequence of nodes, as specified by the grammar. Non-local
     * patterns disable certain optimizations that would otherwise be possible
     * when executing a query on a specific range of a syntax tree.
     *
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getPatternCount pattern count}.
     */
    public boolean isPatternNonLocal(@Unsigned int index) throws IndexOutOfBoundsException {
        checkIndex(index);
        return ts_query_is_pattern_non_local(query, index);
    }

    /**
     * Check if a pattern is guaranteed to match once a given byte offset is reached.
     *
     * @throws IndexOutOfBoundsException If the offset exceeds the source length.
     */
    public boolean isPatternGuaranteedAtStep(@Unsigned int offset) throws IndexOutOfBoundsException {
        if (Integer.compareUnsigned(offset, source.length()) >= 0) {
            throw new IndexOutOfBoundsException(
                    "Byte offset %s exceeds EOF".formatted(Integer.toUnsignedString(offset)));
        }
        return ts_query_is_pattern_guaranteed_at_step(query, offset);
    }

    /**
     * Get the property settings for the given pattern index.
     *
     * <p>Properties are set using the {@code #set!} predicate.
     *
     * @param index The index of a pattern within the query.
     * @return A map of property keys with optional values.
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getPatternCount pattern count}.
     */
    public Map<String, Optional<String>> getPatternSettings(@Unsigned int index) throws IndexOutOfBoundsException {
        checkIndex(index);
        return Collections.unmodifiableMap(settings.get(index));
    }

    /**
     * Get the property assertions for the given pattern index.
     *
     * <p>Assertions are performed using the {@code #is?}
     * (positive) and {@code #is-not?} (negative) predicates.
     *
     * @param index The index of a pattern within the query.
     * @param positive Indicates whether to include positive or negative assertions.
     * @return A map of property keys with optional values.
     * @throws IndexOutOfBoundsException If the index exceeds the
     *                                   {@linkplain #getPatternCount pattern count}.
     */
    public Map<String, Optional<String>> getPatternAssertions(@Unsigned int index, boolean positive)
            throws IndexOutOfBoundsException {
        checkIndex(index);
        var assertions = positive ? positiveAssertions : negativeAssertions;
        return Collections.unmodifiableMap(assertions.get(index));
    }

    /**
     * Iterate over all the matches in the order that they were found. The lifetime of the native memory of the returned
     * matches is bound to the lifetime of this query object.
     *
     * @param node The node that the query will run on.
     */
    public Stream<QueryMatch> findMatches(Node node) {
        return findMatches(node, arena, null);
    }


    /**
     * Iterate over all the matches in the order that they were found. The lifetime of the native memory of the returned
     * matches is bound to the lifetime of this query object.
     *
     * <h4 id="findMatches-example">Predicate Example</h4>
     * <p>
     * {@snippet lang="java" :
     * Stream<QueryMatch> matches = query.findMatches(tree.getRootNode(), (predicate, match) -> {
     *      if (!predicate.getName().equals("ieq?")) return true;
     *      List<QueryPredicateArg> args = predicate.getArgs();
     *      Node node = match.findNodes(args.getFirst().value()).getFirst();
     *      return args.getLast().value().equalsIgnoreCase(node.getText());
     *  });
     * }
     *
     * @param node The node that the query will run on.
     * @param predicate A function that handles custom predicates.
     */
    public Stream<QueryMatch> findMatches(Node node, @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate){
        return findMatches(node, arena, predicate);
    }


    /**
     * Like {@link #findMatches(Node, BiPredicate)} but the native memory of the returned matches is created using the
     * given allocator.
     *
     * @param node The node that the query will run on.
     * @param allocator The allocator that is used to allocate the native memory of the returned matches.
     * @param predicate A function that handles custom predicates.
     */
    public Stream<QueryMatch> findMatches(Node node, SegmentAllocator allocator, @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate) {
        try (var alloc = Arena.ofConfined()) {
            ts_query_cursor_exec(cursor, query, node.copy(alloc));
        }
        return StreamSupport.stream(new MatchesIterator(node.getTree(), predicate, allocator), false);
    }

    @Override
    public void close() throws RuntimeException {
        arena.close();
    }

    @Override
    public String toString() {
        return "Query{language=%s, source=%s}".formatted(language, source);
    }

    private boolean matches(@Nullable BiPredicate<QueryPredicate, QueryMatch> predicate, QueryMatch match) {
        return predicates.get(match.patternIndex()).stream().allMatch(p -> {
            if (p.getClass() != QueryPredicate.class) return p.test(match);
            return predicate == null || predicate.test(p, match);
        });
    }

    private void checkIndex(@Unsigned int index) throws IndexOutOfBoundsException {
        if (Integer.compareUnsigned(index, getPatternCount()) >= 0) {
            throw new IndexOutOfBoundsException(
                    "Pattern index %s is out of bounds".formatted(Integer.toUnsignedString(index)));
        }
    }

    private final class MatchesIterator extends Spliterators.AbstractSpliterator<QueryMatch> {
        private final @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate;
        private final Tree tree;
        private final SegmentAllocator allocator;

        public MatchesIterator(Tree tree, @Nullable BiPredicate<QueryPredicate, QueryMatch> predicate, SegmentAllocator allocator) {
            super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL);
            this.predicate = predicate;
            this.tree = tree;
            this.allocator = allocator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super QueryMatch> action) {
            var hasNoText = tree.getText() == null;
            MemorySegment match = arena.allocate(TSQueryMatch.layout());
            while (ts_query_cursor_next_match(cursor, match)) {
                var count = Short.toUnsignedInt(TSQueryMatch.capture_count(match));
                var matchCaptures = TSQueryMatch.captures(match);
                var captureList = new ArrayList<QueryCapture>(count);
                for (int i = 0; i < count; ++i) {
                    var capture = TSQueryCapture.asSlice(matchCaptures, i);
                    var name = captureNames.get(TSQueryCapture.index(capture));
                    var node = TSNode.allocate(allocator).copyFrom(TSQueryCapture.node(capture));
                    captureList.add(new QueryCapture(name, new Node(node, tree)));
                }
                var patternIndex = TSQueryMatch.pattern_index(match);
                var result = new QueryMatch(patternIndex, captureList);
                if (hasNoText || matches(predicate, result)) {
                    action.accept(result);
                    return true;
                }
            }
            return false;
        }
    }
}
