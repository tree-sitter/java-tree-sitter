package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.TSInputEncodingUTF16BE;
import static io.github.treesitter.jtreesitter.internal.TreeSitter.TSInputEncodingUTF16LE;
import static io.github.treesitter.jtreesitter.internal.TreeSitter.TSInputEncodingUTF8;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NonNull;

/** The encoding of source code. */
public enum InputEncoding {
    /** UTF-8 encoding. */
    UTF_8(StandardCharsets.UTF_8, TSInputEncodingUTF8()),
    /**
     * UTF-16 little endian encoding.
     *
     * @since 0.25.0
     */
    UTF_16LE(StandardCharsets.UTF_16LE, TSInputEncodingUTF16LE()),
    /**
     * UTF-16 big endian encoding.
     *
     * @since 0.25.0
     */
    UTF_16BE(StandardCharsets.UTF_16BE, TSInputEncodingUTF16BE());

    private final @NonNull Charset charset;
    private final int tsInputEncoding;

    InputEncoding(@NonNull Charset charset, int tsInputEncoding) {
        this.charset = charset;
        this.tsInputEncoding = tsInputEncoding;
    }

    Charset charset() {
        return charset;
    }

    int tsInputEncoding() {
        return tsInputEncoding;
    }

    private static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    /**
     * Convert a standard {@linkplain Charset} to an {@linkplain InputEncoding}.
     *
     * @param charset one of {@link StandardCharsets#UTF_8}, {@link StandardCharsets#UTF_16BE},
     *                {@link StandardCharsets#UTF_16LE}, or {@link StandardCharsets#UTF_16} (native byte order).
     * @throws IllegalArgumentException If the character set is invalid.
     */
    @SuppressWarnings("SameParameterValue")
    public static @NonNull InputEncoding valueOf(@NonNull Charset charset) throws IllegalArgumentException {
        if (charset.equals(StandardCharsets.UTF_8)) return InputEncoding.UTF_8;
        if (charset.equals(StandardCharsets.UTF_16BE)) return InputEncoding.UTF_16BE;
        if (charset.equals(StandardCharsets.UTF_16LE)) return InputEncoding.UTF_16LE;
        if (charset.equals(StandardCharsets.UTF_16)) {
            return IS_BIG_ENDIAN ? InputEncoding.UTF_16BE : InputEncoding.UTF_16LE;
        }
        throw new IllegalArgumentException("Invalid character set: %s".formatted(charset));
    }
}
