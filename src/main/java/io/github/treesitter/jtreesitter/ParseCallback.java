package io.github.treesitter.jtreesitter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A function that retrieves a chunk of text at a given byte offset and point. */
@FunctionalInterface
public interface ParseCallback {
    /**
     * Applies this function to the given arguments.
     *
     * @param offset the current byte offset
     * @param point the current point
     * @return A chunk of text or {@code null} to indicate the end of the document.
     */
    @Nullable
    String read(@Unsigned int offset, @NonNull Point point);

    /**
     * Applies this function to the given arguments.
     *
     * @param offset the current byte offset
     * @param point the current point
     * @return A chunk of text or {@code null} to indicate the end of the document.
     * @deprecated Use {@link #read(int, Point)} instead
     */
    @Nullable
    @Deprecated(since = "0.26.0", forRemoval = true)
    default String apply(@Unsigned Integer offset, @NonNull Point point) {
        return read(offset, point);
    }
}
