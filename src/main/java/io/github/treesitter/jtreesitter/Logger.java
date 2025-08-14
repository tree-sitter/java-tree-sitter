package io.github.treesitter.jtreesitter;

import org.jspecify.annotations.NonNull;

/** A function that logs parsing results. */
@FunctionalInterface
public interface Logger {
    /**
     * Performs this operation on the given arguments.
     *
     * @param type the log type
     * @param message the log message
     */
    void log(@NonNull Type type, @NonNull String message);

    /**
     * Performs this operation on the given arguments.
     *
     * @param type the log type
     * @param message the log message
     * @deprecated Use {@link #log(Logger.Type, String)} instead
     */
    @Deprecated(since = "0.26.0", forRemoval = true)
    default void accept(@NonNull Type type, @NonNull String message) {
        log(type, message);
    }

    /** The type of a log message. */
    enum Type {
        /** Lexer message. */
        LEX,
        /** Parser message. */
        PARSE
    }
}
