package io.github.treesitter.jtreesitter;

import org.jspecify.annotations.NullMarked;

/**
 * The metadata associated with a {@linkplain Language}.
 *
 * @since 0.25.0
 */
@NullMarked
public record LanguageMetadata(Version version) {
    /**
     * The <a href="https://semver.org/">Semantic Version</a> of the {@linkplain Language}.
     *
     * <p>This version information may be used to signal if a given parser
     * is incompatible with existing queries when upgrading between versions.
     *
     * @since 0.25.0
     */
    public record Version(@Unsigned short major, @Unsigned short minor, @Unsigned short patch) {
        @Override
        public String toString() {
            return "%d.%d.%d".formatted(major, minor, patch);
        }
    }
}
