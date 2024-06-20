package io.github.treesitter.jtreesitter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NonNull;

/** The encoding of source code. */
public enum InputEncoding {
    /** UTF-8 encoding. */
    UTF_8(StandardCharsets.UTF_8),
    /** UTF-16 encoding. */
    UTF_16(StandardCharsets.UTF_16BE);

    private final @NonNull Charset charset;

    InputEncoding(@NonNull Charset charset) {
        this.charset = charset;
    }

    Charset charset() {
        return charset;
    }

    /**
     * Convert a standard {@linkplain Charset} to an {@linkplain InputEncoding}.
     *
     * @param charset one of {@link StandardCharsets#UTF_8} or {@link StandardCharsets#UTF_16BE}
     * @throws IllegalArgumentException If the character set is invalid.
     */
    static @NonNull InputEncoding valueOf(@NonNull Charset charset) throws IllegalArgumentException {
        if (charset.equals(StandardCharsets.UTF_8)) return InputEncoding.UTF_8;
        if (charset.equals(StandardCharsets.UTF_16BE)) return InputEncoding.UTF_16;
        throw new IllegalArgumentException("Invalid character set: %s".formatted(charset));
    }
}
