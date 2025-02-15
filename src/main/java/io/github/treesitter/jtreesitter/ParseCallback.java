package io.github.treesitter.jtreesitter;

import java.util.function.BiFunction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A function that retrieves a chunk of text at a given byte offset and point. */
@FunctionalInterface
public interface ParseCallback extends BiFunction<Integer, Point, String> {
    /**
     * {@inheritDoc}
     *
     * @param offset the current byte offset
     * @param point the current point
     * @return A chunk of text or {@code null} to indicate the end of the document.
     */
    @Override
    @Nullable
    String apply(@Unsigned Integer offset, @NonNull Point point);
}
