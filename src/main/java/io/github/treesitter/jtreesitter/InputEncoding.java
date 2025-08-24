package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NonNull;

/** The encoding of source code. */
@SuppressWarnings("ClassCanBeRecord")
public class InputEncoding {
    private final @NonNull Charset charset;

    private final int encoding;

    private InputEncoding(@NonNull Charset charset, int encoding) {
        this.charset = charset;
        this.encoding = encoding;
    }

    Charset charset() {
        return charset;
    }

    int encoding() {
        return encoding;
    }

    /** UTF-8 encoding. */
    public static final InputEncoding UTF_8 = new InputEncoding(StandardCharsets.UTF_8, TSInputEncodingUTF8());

    /**
     * UTF-16 little endian encoding.
     *
     * @since 0.25.0
     */
    public static final InputEncoding UTF_16LE = new InputEncoding(StandardCharsets.UTF_16LE, TSInputEncodingUTF16LE());

    /**
     * UTF-16 big endian encoding.
     *
     * @since 0.25.0
     */
    public static final InputEncoding UTF_16BE = new InputEncoding(StandardCharsets.UTF_16BE, TSInputEncodingUTF16BE());

    private static final boolean IS_BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    /**
     * Convert a standard {@linkplain Charset} to an {@linkplain InputEncoding}.
     *
     * @implNote The following encodings are handled by the Tree-sitter library:
     *           {@link StandardCharsets#UTF_8}, {@link StandardCharsets#UTF_16BE},
     *           {@link StandardCharsets#UTF_16LE}, and {@link StandardCharsets#UTF_16} (native byte order).
     *           Every other {@link Charset} will use its {@link Charset#decode(ByteBuffer) decoder}
     *           ({@link StandardCharsets#UTF_32} is converted to the native byte order).
     *
     * @since 0.26.0
     */
    @SuppressWarnings("SameParameterValue")
    public static @NonNull InputEncoding valueOf(@NonNull Charset charset) throws IllegalArgumentException {
        if (charset.equals(StandardCharsets.UTF_8)) return InputEncoding.UTF_8;
        if (charset.equals(StandardCharsets.UTF_16BE)) return InputEncoding.UTF_16BE;
        if (charset.equals(StandardCharsets.UTF_16LE)) return InputEncoding.UTF_16LE;
        if (charset.equals(StandardCharsets.UTF_16)) {
            return IS_BIG_ENDIAN ? InputEncoding.UTF_16BE : InputEncoding.UTF_16LE;
        }
        if (charset.equals(StandardCharsets.UTF_32)) {
            charset = IS_BIG_ENDIAN ? StandardCharsets.UTF_32BE : StandardCharsets.UTF_32LE;
        }
        return new InputEncoding(charset, TSInputEncodingCustom());
    }
}
