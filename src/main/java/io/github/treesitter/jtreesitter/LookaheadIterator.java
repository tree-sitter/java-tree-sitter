package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.TreeSitter;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NullMarked;

/**
 * A class that is used to look up valid symbols in a specific parse state.
 *
 * <p>Lookahead iterators can be useful to generate suggestions and improve syntax error diagnostics.<br>
 * To get symbols  valid in an {@index ERROR} node, use the lookahead iterator on its first leaf node state.<br>
 * For {@index MISSING} nodes, a lookahead iterator created on the previous non-extra leaf node may be appropriate.
 */
@NullMarked
public final class LookaheadIterator implements AutoCloseable, Iterator<LookaheadIterator.Symbol> {
    private final Arena arena;
    private final MemorySegment self;
    private final short state;
    private boolean iterFirst = true;
    private boolean hasNext = false;

    LookaheadIterator(MemorySegment language, @Unsigned short state) throws IllegalArgumentException {
        var iterator = ts_lookahead_iterator_new(language, state);
        if (iterator == null) {
            throw new IllegalArgumentException(
                    "State %d is not valid for %s".formatted(Short.toUnsignedInt(state), this));
        }
        this.state = state;
        arena = Arena.ofShared();
        self = iterator.reinterpret(arena, TreeSitter::ts_lookahead_iterator_delete);
    }

    /** Get the current language of the lookahead iterator. */
    public Language getLanguage() {
        return new Language(ts_lookahead_iterator_language(self));
    }

    /**
     * Get the current symbol ID.
     *
     * @apiNote The ID of the {@index ERROR} symbol is equal to {@code -1}.
     */
    public @Unsigned short getCurrentSymbol() {
        return ts_lookahead_iterator_current_symbol(self);
    }

    /**
     * The current symbol name.
     *
     * @apiNote Newly created lookahead iterators will contain the {@index ERROR} symbol.
     */
    public String getCurrentSymbolName() {
        return ts_lookahead_iterator_current_symbol_name(self).getString(0);
    }

    /**
     * Reset the lookahead iterator to the given state.
     *
     * @return {@code true} if the iterator was reset
     *         successfully or {@code false} if it failed.
     */
    public boolean reset(@Unsigned short state) {
        return ts_lookahead_iterator_reset_state(self, state);
    }

    /**
     * Reset the lookahead iterator to the given state and another language.
     *
     * @return {@code true} if the iterator was reset
     *         successfully or {@code false} if it failed.
     */
    public boolean reset(@Unsigned short state, Language language) {
        return ts_lookahead_iterator_reset(self, language.segment(), state);
    }

    /** Check if the lookahead iterator has more symbols. */
    @Override
    public boolean hasNext() {
        if (iterFirst) {
            iterFirst = false;
            hasNext = ts_lookahead_iterator_next(self);
            ts_lookahead_iterator_reset_state(self, state);
        }
        return hasNext;
    }

    /**
     * Advance the lookahead iterator to the next symbol.
     *
     * @throws NoSuchElementException If there are no more symbols.
     */
    @Override
    public Symbol next() throws NoSuchElementException {
        if (!hasNext()) throw new NoSuchElementException();
        hasNext = ts_lookahead_iterator_next(self);
        return new Symbol(getCurrentSymbol(), getCurrentSymbolName());
    }

    /**
     * Iterate over the symbol IDs.
     *
     * @implNote Calling this method will reset the iterator to its original state.
     */
    public @Unsigned Stream<Short> symbols() {
        ts_lookahead_iterator_reset_state(self, state);
        return StreamSupport.stream(new IdIterator(self), false);
    }

    /**
     * Iterate over the symbol names.
     *
     * @implNote Calling this method will reset the iterator to its original state.
     */
    public Stream<String> names() {
        ts_lookahead_iterator_reset_state(self, state);
        return StreamSupport.stream(new NameIterator(self), false);
    }

    @Override
    public void close() throws RuntimeException {
        arena.close();
    }

    /** @hidden */
    @Override
    public void remove() {
        Iterator.super.remove();
    }

    /** A class that pairs a symbol ID with its name. */
    public record Symbol(@Unsigned short id, String name) {}

    private static final class IdIterator extends Spliterators.AbstractSpliterator<Short> {
        private final MemorySegment iterator;

        private IdIterator(MemorySegment iterator) {
            super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.SORTED | Spliterator.ORDERED);
            this.iterator = iterator;
        }

        @Override
        public Comparator<? super Short> getComparator() {
            return Short::compareUnsigned;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Short> action) {
            var result = ts_lookahead_iterator_next(iterator);
            if (result) {
                var symbol = ts_lookahead_iterator_current_symbol(iterator);
                action.accept(symbol);
            }
            return result;
        }
    }

    private static final class NameIterator extends Spliterators.AbstractSpliterator<String> {
        private final MemorySegment iterator;

        private NameIterator(MemorySegment iterator) {
            super(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL);
            this.iterator = iterator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            var result = ts_lookahead_iterator_next(iterator);
            if (result) {
                var name = ts_lookahead_iterator_current_symbol_name(iterator);
                action.accept(name.getString(0));
            }
            return result;
        }
    }
}
