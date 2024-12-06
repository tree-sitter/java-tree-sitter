package io.github.treesitter.jtreesitter.internal;

import io.github.treesitter.jtreesitter.NativeLibraryLookup;
import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.util.Optional;
import java.util.ServiceLoader;

final class ChainedLibraryLookup implements NativeLibraryLookup {
    @Override
    public SymbolLookup get(Arena arena) {
        var serviceLoader = ServiceLoader.load(NativeLibraryLookup.class);
        SymbolLookup lookup = (name) -> Optional.empty();
        for (var libraryLookup : serviceLoader) {
            lookup = lookup.or(libraryLookup.get(arena));
        }
        return lookup.or(findLibrary(arena)).or(Linker.nativeLinker().defaultLookup());
    }

    private SymbolLookup findLibrary(Arena arena) {
        try {
            String library = System.mapLibraryName("tree-sitter");
            return SymbolLookup.libraryLookup(library, arena);
        } catch (IllegalArgumentException e) {
            return SymbolLookup.loaderLookup();
        }
    }
}
