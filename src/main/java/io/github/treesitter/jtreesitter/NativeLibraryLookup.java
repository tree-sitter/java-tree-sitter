package io.github.treesitter.jtreesitter;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;

/**
 * An interface implemented by clients that wish to customize the {@link SymbolLookup} used for the tree-sitter
 * native library. Implementation classes must be registered by listing their fully qualified class name
 * in a resource file named {@code META-INF/services/io.github.treesitter.jtreesitter.NativeLibraryLookup}.
 *
 * @see java.util.ServiceLoader
 */
public interface NativeLibraryLookup {
    /**
     * Returns the {@link SymbolLookup} to be used for the tree-sitter native library.
     *
     * @param arena an arena for the symbol lookup
     * @return the {@link SymbolLookup} to be used for the tree-sitter native library
     */
    SymbolLookup get(Arena arena);
}
