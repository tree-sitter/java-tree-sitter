package io.github.treesitter.jtreesitter.languages;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class TreeSitterJava {
    private static final ValueLayout VOID_PTR =
            ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE));
    private static final FunctionDescriptor FUNC_DESC = FunctionDescriptor.of(VOID_PTR);
    private static final Linker LINKER = Linker.nativeLinker();
    private static final TreeSitterJava INSTANCE = new TreeSitterJava();

    private final Arena arena = Arena.ofAuto();
    private final SymbolLookup symbols = findLibrary();

    /**
     * {@snippet lang=c :
     * const TSLanguage *tree_sitter_java()
     * }
     */
    public static MemorySegment language() {
        return INSTANCE.call("tree_sitter_java");
    }

    private SymbolLookup findLibrary() {
        try {
            var library = System.mapLibraryName("tree-sitter-java");
            return SymbolLookup.libraryLookup(library, arena);
        } catch (IllegalArgumentException e) {
            return SymbolLookup.loaderLookup();
        }
    }

    private static UnsatisfiedLinkError unresolved(String name) {
        return new UnsatisfiedLinkError("Unresolved symbol: %s".formatted(name));
    }

    @SuppressWarnings("SameParameterValue")
    private MemorySegment call(String name) throws UnsatisfiedLinkError {
        var address = symbols.find(name).orElseThrow(() -> unresolved(name));
        try {
            var function = LINKER.downcallHandle(address, FUNC_DESC);
            var languagePointer = (MemorySegment) function.invokeExact();
            // The results of Linker downcalls always use the global scope, but the language pointer actually points
            // to data in the loaded library. Therefore change the scope of the pointer to be the same as the library.
            // So if the library is unloaded while the language pointer is still in use, the language pointer becomes
            // invalid and an exception occurs (instead of a JVM crash).
            languagePointer = languagePointer.reinterpret(arena, ignored -> {});
            return languagePointer.asReadOnly();
        } catch (Throwable e) {
            throw new RuntimeException("Call to %s failed".formatted(name), e);
        }
    }
}
