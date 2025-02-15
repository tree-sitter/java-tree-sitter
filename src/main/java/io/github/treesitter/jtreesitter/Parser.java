package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** A class that is used to produce a {@linkplain Tree syntax tree} from source code. */
@NullMarked
public final class Parser implements AutoCloseable {
    final MemorySegment self;
    private final Arena arena;
    private @Nullable Language language;
    private List<Range> includedRanges = Collections.singletonList(Range.DEFAULT);

    /**
     * Creates a new instance with a {@code null} language.
     *
     * @apiNote Parsing cannot be performed while the language is {@code null}.
     */
    public Parser() {
        arena = Arena.ofShared();
        self = ts_parser_new().reinterpret(arena, TreeSitter::ts_parser_delete);
    }

    /** Creates a new instance with the given language. */
    public Parser(Language language) {
        this();
        ts_parser_set_language(self, language.segment());
        this.language = language;
    }

    /** Get the language that the parser will use for parsing. */
    public @Nullable Language getLanguage() {
        return language;
    }

    /** Set the language that the parser will use for parsing. */
    public Parser setLanguage(Language language) {
        ts_parser_set_language(self, language.segment());
        this.language = language;
        return this;
    }

    /**
     * Get the maximum duration in microseconds that
     * parsing should be allowed to take before halting.
     *
     * @deprecated Use {@link Options} instead.
     */
    @Deprecated(since = "0.25.0")
    public @Unsigned long getTimeoutMicros() {
        return ts_parser_timeout_micros(self);
    }

    /**
     * Set the maximum duration in microseconds that
     * parsing should be allowed to take before halting.
     *
     * @deprecated Use {@link Options} instead.
     */
    @Deprecated(since = "0.25.0")
    @SuppressWarnings("DeprecatedIsStillUsed")
    public Parser setTimeoutMicros(@Unsigned long timeoutMicros) {
        ts_parser_set_timeout_micros(self, timeoutMicros);
        return this;
    }

    /**
     * Set the logger that the parser will use during parsing.
     *
     * <h4 id="logger-example">Example</h4>
     * <p>
     * {@snippet lang="java" :
     * import java.util.logging.Logger;
     *
     * Logger logger = Logger.getLogger("tree-sitter");
     * Parser parser = new Parser().setLogger(
     *    (type, message) -> logger.info("%s - %s".formatted(type.name(), message)));
     * }
     */
    @SuppressWarnings("unused")
    public Parser setLogger(@Nullable Logger logger) {
        if (logger == null) {
            ts_parser_set_logger(self, TSLogger.allocate(arena));
        } else {
            var segment = TSLogger.allocate(arena);
            TSLogger.payload(segment, MemorySegment.NULL);
            // NOTE: can't use _ because of palantir/palantir-java-format#934
            var log = TSLogger.log.allocate(
                    (p, type, message) -> {
                        var logType = Logger.Type.values()[type];
                        logger.accept(logType, message.getString(0));
                    },
                    arena);
            TSLogger.log(segment, log);
            ts_parser_set_logger(self, segment);
        }
        return this;
    }

    /**
     * Set the parser's current cancellation flag.
     *
     * <p>The parser will periodically read from this flag during parsing.
     * If it reads a non-zero value, it will halt early.
     *
     * @deprecated Use {@link Options} instead.
     */
    @Deprecated(since = "0.25.0")
    @SuppressWarnings("DeprecatedIsStillUsed")
    public synchronized Parser setCancellationFlag(CancellationFlag cancellationFlag) {
        ts_parser_set_cancellation_flag(self, cancellationFlag.segment);
        return this;
    }

    /**
     * Get the ranges of text that the parser should include when parsing.
     *
     * @apiNote By default, the parser will always include entire documents.
     */
    public List<Range> getIncludedRanges() {
        return includedRanges;
    }

    /**
     * Set the ranges of text that the parser should include when parsing.
     *
     * <p>This allows you to parse only a <em>portion</em> of a document
     * but still return a syntax tree whose ranges match up with the
     * document as a whole. You can also pass multiple disjoint ranges.
     *
     * @throws IllegalArgumentException If the ranges overlap or are not in ascending order.
     */
    public Parser setIncludedRanges(List<Range> includedRanges) {
        var size = includedRanges.size();
        if (size > 0) {
            try (var arena = Arena.ofConfined()) {
                var layout = MemoryLayout.sequenceLayout(size, TSRange.layout());
                var ranges = arena.allocate(layout);

                var startRow = layout.varHandle(
                        MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.groupElement("start_point"),
                        MemoryLayout.PathElement.groupElement("row"));
                var startColumn = layout.varHandle(
                        MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.groupElement("start_point"),
                        MemoryLayout.PathElement.groupElement("column"));
                var endRow = layout.varHandle(
                        MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.groupElement("end_point"),
                        MemoryLayout.PathElement.groupElement("row"));
                var endColumn = layout.varHandle(
                        MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.groupElement("end_point"),
                        MemoryLayout.PathElement.groupElement("column"));
                var startByte = layout.varHandle(
                        MemoryLayout.PathElement.sequenceElement(),
                        MemoryLayout.PathElement.groupElement("start_byte"));
                var endByte = layout.varHandle(
                        MemoryLayout.PathElement.sequenceElement(), /**/
                        MemoryLayout.PathElement.groupElement("end_byte"));

                for (int i = 0; i < size; ++i) {
                    var range = includedRanges.get(i).into(arena);
                    var startPoint = TSRange.start_point(range);
                    var endPoint = TSRange.end_point(range);
                    startByte.set(ranges, 0L, (long) i, TSRange.start_byte(range));
                    endByte.set(ranges, 0L, (long) i, TSRange.end_byte(range));
                    startRow.set(ranges, 0L, (long) i, TSPoint.row(startPoint));
                    startColumn.set(ranges, 0L, (long) i, TSPoint.column(startPoint));
                    endRow.set(ranges, 0L, (long) i, TSPoint.row(endPoint));
                    endColumn.set(ranges, 0L, (long) i, TSPoint.column(endPoint));
                }

                if (!ts_parser_set_included_ranges(self, ranges, size)) {
                    throw new IllegalArgumentException(
                            "Included ranges must be in ascending order and must not overlap");
                }
            }
            this.includedRanges = List.copyOf(includedRanges);
        } else {
            ts_parser_set_included_ranges(self, MemorySegment.NULL, 0);
            this.includedRanges = Collections.singletonList(Range.DEFAULT);
        }
        return this;
    }

    /**
     * Parse source code from a string and create a syntax tree.
     *
     * @return An optional {@linkplain Tree} which is empty if parsing was halted.
     * @throws IllegalStateException If the parser does not have a language assigned.
     */
    public Optional<Tree> parse(String source) throws IllegalStateException {
        return parse(source, InputEncoding.UTF_8);
    }

    /**
     * Parse source code from a string and create a syntax tree.
     *
     * @return An optional {@linkplain Tree} which is empty if parsing was halted.
     * @throws IllegalStateException If the parser does not have a language assigned.
     */
    public Optional<Tree> parse(String source, InputEncoding encoding) throws IllegalStateException {
        return parse(source, encoding, null);
    }

    /**
     * Parse source code from a string and create a syntax tree.
     *
     * <p>If you have already parsed an earlier version of this document and the
     * document has since been edited, pass the previous syntax tree to {@code oldTree}
     * so that the unchanged parts of it can be reused. This will save time and memory.
     * <br>For this to work correctly, you must have already edited the old syntax tree using
     * the {@link Tree#edit} method in a way that exactly matches the source code changes.
     *
     * @return An optional {@linkplain Tree} which is empty if parsing was halted.
     * @throws IllegalStateException If the parser does not have a language assigned.
     */
    public Optional<Tree> parse(String source, Tree oldTree) throws IllegalStateException {
        return parse(source, InputEncoding.UTF_8, oldTree);
    }

    /**
     * Parse source code from a string and create a syntax tree.
     *
     * <p>If you have already parsed an earlier version of this document and the
     * document has since been edited, pass the previous syntax tree to {@code oldTree}
     * so that the unchanged parts of it can be reused. This will save time and memory.
     * <br>For this to work correctly, you must have already edited the old syntax tree using
     * the {@link Tree#edit} method in a way that exactly matches the source code changes.
     *
     * @return An optional {@linkplain Tree} which is empty if parsing was halted.
     * @throws IllegalStateException If the parser does not have a language assigned.
     */
    public Optional<Tree> parse(String source, InputEncoding encoding, @Nullable Tree oldTree)
            throws IllegalStateException {
        if (language == null) {
            throw new IllegalStateException("The parser has no language assigned");
        }

        try (var alloc = Arena.ofShared()) {
            var bytes = source.getBytes(encoding.charset());
            var string = alloc.allocateFrom(C_CHAR, bytes);
            var old = oldTree == null ? MemorySegment.NULL : oldTree.segment();
            var tree = ts_parser_parse_string_encoding(self, old, string, bytes.length, encoding.ordinal());
            if (tree.equals(MemorySegment.NULL)) return Optional.empty();
            return Optional.of(new Tree(tree, language, source, encoding.charset()));
        }
    }

    /**
     * Parse source code from a callback and create a syntax tree.
     *
     * @return An optional {@linkplain Tree} which is empty if parsing was halted.
     * @throws IllegalStateException If the parser does not have a language assigned.
     */
    public Optional<Tree> parse(ParseCallback parseCallback, InputEncoding encoding) throws IllegalStateException {
        return parse(parseCallback, encoding, null, null);
    }

    /**
     * Parse source code from a callback and create a syntax tree.
     *
     * @return An optional {@linkplain Tree} which is empty if parsing was halted.
     * @throws IllegalStateException If the parser does not have a language assigned.
     */
    public Optional<Tree> parse(ParseCallback parseCallback, InputEncoding encoding, Options options)
            throws IllegalStateException {
        return parse(parseCallback, encoding, null, options);
    }

    /**
     * Parse source code from a callback and create a syntax tree.
     *
     * <p>If you have already parsed an earlier version of this document and the
     * document has since been edited, pass the previous syntax tree to {@code oldTree}
     * so that the unchanged parts of it can be reused. This will save time and memory.
     * <br>For this to work correctly, you must have already edited the old syntax tree using
     * the {@link Tree#edit} method in a way that exactly matches the source code changes.
     *
     * @return An optional {@linkplain Tree} which is empty if parsing was halted.
     * @throws IllegalStateException If the parser does not have a language assigned.
     */
    @SuppressWarnings("unused")
    public Optional<Tree> parse(
            ParseCallback parseCallback, InputEncoding encoding, @Nullable Tree oldTree, @Nullable Options options)
            throws IllegalStateException {
        if (language == null) {
            throw new IllegalStateException("The parser has no language assigned");
        }

        var input = TSInput.allocate(arena);
        TSInput.payload(input, MemorySegment.NULL);
        TSInput.encoding(input, encoding.ordinal());
        // NOTE: can't use _ because of palantir/palantir-java-format#934
        var read = TSInput.read.allocate(
                (payload, index, point, bytes) -> {
                    var result = parseCallback.apply(index, Point.from(point));
                    if (result == null) {
                        bytes.set(C_INT, 0, 0);
                        return MemorySegment.NULL;
                    }
                    var buffer = result.getBytes(encoding.charset());
                    bytes.set(C_INT, 0, buffer.length);
                    return arena.allocateFrom(C_CHAR, buffer);
                },
                arena);
        TSInput.read(input, read);

        MemorySegment tree, old = oldTree == null ? MemorySegment.NULL : oldTree.segment();
        if (options == null) {
            tree = ts_parser_parse(self, old, input);
        } else {
            var parseOptions = TSParseOptions.allocate(arena);
            TSParseOptions.payload(parseOptions, MemorySegment.NULL);
            var progress = TSParseOptions.progress_callback.allocate(
                    (payload) -> {
                        var offset = TSParseState.current_byte_offset(payload);
                        var hasError = TSParseState.has_error(payload);
                        return options.progressCallback(new State(offset, hasError));
                    },
                    arena);
            TSParseOptions.progress_callback(parseOptions, progress);
            tree = ts_parser_parse_with_options(self, old, input, parseOptions);
        }
        if (tree.equals(MemorySegment.NULL)) return Optional.empty();
        return Optional.of(new Tree(tree, language, null, null));
    }

    /**
     * Instruct the parser to start the next {@linkplain #parse parse} from the beginning.
     *
     * @apiNote If parsing was previously halted, the parser will resume where it left off.
     * If you intend to parse another document instead, you must call this method first.
     */
    public void reset() {
        ts_parser_reset(self);
    }

    @Override
    public void close() throws RuntimeException {
        arena.close();
    }

    @Override
    public String toString() {
        return "Parser{language=%s}".formatted(language);
    }

    /**
     * A class representing the current state of the parser.
     *
     * @since 0.25.0
     */
    public static final class State {
        private final @Unsigned int currentByteOffset;
        private final boolean hasError;

        private State(@Unsigned int currentByteOffset, boolean hasError) {
            this.currentByteOffset = currentByteOffset;
            this.hasError = hasError;
        }

        /** Get the current byte offset of the parser. */
        public @Unsigned int getCurrentByteOffset() {
            return currentByteOffset;
        }

        /** Check if the parser has encountered an error. */
        public boolean hasError() {
            return hasError;
        }

        @Override
        public String toString() {
            return String.format(
                    "Parser.State{currentByteOffset=%s, hasError=%s}",
                    Integer.toUnsignedString(currentByteOffset), hasError);
        }
    }

    /**
     * A class representing the parser options.
     *
     * @since 0.25.0
     */
    @NullMarked
    public static final class Options {
        private final Predicate<State> progressCallback;

        public Options(Predicate<State> progressCallback) {
            this.progressCallback = progressCallback;
        }

        private boolean progressCallback(State state) {
            return progressCallback.test(state);
        }
    }

    /**
     * A class representing a cancellation flag.
     *
     * @deprecated Use {@link Options} instead.
     */
    @Deprecated(since = "0.25.0")
    @SuppressWarnings("DeprecatedIsStillUsed")
    public static class CancellationFlag {
        private final Arena arena = Arena.ofAuto();
        private final MemorySegment segment = arena.allocate(C_LONG_LONG);
        private final AtomicLong value = new AtomicLong();

        /** Creates an uninitialized cancellation flag. */
        public CancellationFlag() {}

        /** Get the value of the flag. */
        public long get() {
            return value.get();
        }

        /** Set the value of the flag. */
        @SuppressWarnings("unused")
        public void set(long value) {
            // NOTE: can't use _ because of palantir/palantir-java-format#934
            segment.set(C_LONG_LONG, 0L, this.value.updateAndGet(o -> value));
        }
    }
}
