package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.TSLanguageMetadata;
import java.lang.foreign.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** A class that defines how to parse a particular language. */
@NullMarked
public final class Language implements Cloneable {
    /**
     * The latest ABI version that is supported by the current version of the library.
     *
     * @apiNote The Tree-sitter library is generally backwards-compatible with
     * languages generated using older CLI versions, but is not forwards-compatible.
     */
    public static final @Unsigned int LANGUAGE_VERSION = TREE_SITTER_LANGUAGE_VERSION();

    /** The earliest ABI version that is supported by the current version of the library. */
    public static final @Unsigned int MIN_COMPATIBLE_LANGUAGE_VERSION = TREE_SITTER_MIN_COMPATIBLE_LANGUAGE_VERSION();

    private static final ValueLayout VOID_PTR =
            ValueLayout.ADDRESS.withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE));

    private static final FunctionDescriptor FUNC_DESC = FunctionDescriptor.of(VOID_PTR);

    private static final Linker LINKER = Linker.nativeLinker();

    private final MemorySegment self;

    private final @Unsigned int version;

    /**
     * Creates a new instance from the given language pointer.
     *
     * @implNote It is up to the caller to ensure that the pointer is valid.
     *
     * @throws IllegalArgumentException If the language version is incompatible.
     */
    public Language(MemorySegment self) throws IllegalArgumentException {
        this.self = self.asReadOnly();
        version = ts_language_abi_version(this.self);
        if (version < MIN_COMPATIBLE_LANGUAGE_VERSION || version > LANGUAGE_VERSION) {
            throw new IllegalArgumentException(String.format(
                    "Incompatible language version %d. Must be between %d and %d.",
                    version, MIN_COMPATIBLE_LANGUAGE_VERSION, LANGUAGE_VERSION));
        }
    }

    private static UnsatisfiedLinkError unresolved(String name) {
        return new UnsatisfiedLinkError("Unresolved symbol: %s".formatted(name));
    }

    /**
     * Load a language by looking for its function in the given symbols.
     *
     * <h4 id="load-example">Example</h4>
     *
     * <p>{@snippet lang="java" :
     * String library = System.mapLibraryName("tree-sitter-java");
     * SymbolLookup symbols = SymbolLookup.libraryLookup(library, Arena.global());
     * Language language = Language.load(symbols, "tree_sitter_java");
     * }
     * <p>
     * <strong>The {@linkplain Arena} used to load the language
     * must not be closed while the language is being used.</strong>
     *
     * @throws RuntimeException If the language could not be loaded.
     * @since 0.23.1
     */
    // TODO: deprecate when the bindings are generated by the CLI
    public static Language load(SymbolLookup symbols, String language) throws RuntimeException {
        var address = symbols.find(language).orElseThrow(() -> unresolved(language));
        try {
            var function = LINKER.downcallHandle(address, FUNC_DESC);
            return new Language((MemorySegment) function.invokeExact());
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load %s".formatted(language), e);
        }
    }

    MemorySegment segment() {
        return self;
    }

    /**
     * Get the ABI version number for this language.
     *
     * <p>This version number is used to ensure that languages
     * were generated by a compatible version of Tree-sitter.
     *
     * @since 0.25.0
     */
    public @Unsigned int getAbiVersion() {
        return version;
    }

    /**
     * Get the ABI version number for this language.
     *
     * @deprecated Use {@link #getAbiVersion} instead.
     */
    @Deprecated(since = "0.25.0", forRemoval = true)
    public @Unsigned int getVersion() {
        return version;
    }

    /** Get the name of this language, if available. */
    public @Nullable String getName() {
        var name = ts_language_name(self);
        return name.equals(MemorySegment.NULL) ? null : name.getString(0);
    }

    /**
     * Get the metadata for this language, if available.
     *
     * @apiNote This information is generated by the Tree-sitter
     * CLI and relies on the language author providing the correct
     * metadata in the language's {@code tree-sitter.json} file.
     *
     * @since 0.25.0
     */
    public @Nullable LanguageMetadata getMetadata() {
        var metadata = ts_language_metadata(self);
        if (metadata.equals(MemorySegment.NULL)) return null;

        short major = TSLanguageMetadata.major_version(metadata);
        short minor = TSLanguageMetadata.minor_version(metadata);
        short patch = TSLanguageMetadata.patch_version(metadata);
        var version = new LanguageMetadata.Version(major, minor, patch);
        return new LanguageMetadata(version);
    }

    /** Get the number of distinct node types in this language. */
    public @Unsigned int getSymbolCount() {
        return ts_language_symbol_count(self);
    }

    /** Get the number of valid states in this language */
    public @Unsigned int getStateCount() {
        return ts_language_state_count(self);
    }

    /** Get the number of distinct field names in this language */
    public @Unsigned int getFieldCount() {
        return ts_language_field_count(self);
    }

    /**
     * Get all supertype symbols for the language.
     *
     * @since 0.25.0
     */
    public @Unsigned short[] getSupertypes() {
        try (var alloc = Arena.ofConfined()) {
            var length = alloc.allocate(C_INT.byteSize(), C_INT.byteAlignment());
            var supertypes = ts_language_supertypes(self, length);
            var isEmpty = length.get(C_INT, 0) == 0;
            return isEmpty ? new short[0] : supertypes.toArray(C_SHORT);
        }
    }

    /**
     * Get all symbols for a given supertype symbol.
     *
     * @since 0.25.0
     * @see #getSupertypes()
     */
    public @Unsigned short[] getSubtypes(@Unsigned short supertype) {
        try (var alloc = Arena.ofConfined()) {
            var length = alloc.allocate(C_INT.byteSize(), C_INT.byteAlignment());
            var subtypes = ts_language_subtypes(self, supertype, length);
            var isEmpty = length.get(C_INT, 0) == 0;
            return isEmpty ? new short[0] : subtypes.toArray(C_SHORT);
        }
    }

    /** Get the node type for the given numerical ID. */
    public @Nullable String getSymbolName(@Unsigned short symbol) {
        var name = ts_language_symbol_name(self, symbol);
        return name.equals(MemorySegment.NULL) ? null : name.getString(0);
    }

    /** Get the numerical ID for the given node type, or {@code 0} if not found. */
    public @Unsigned short getSymbolForName(String name, boolean isNamed) {
        try (var arena = Arena.ofConfined()) {
            var segment = arena.allocateFrom(name);
            return ts_language_symbol_for_name(self, segment, name.length(), isNamed);
        }
    }

    /**
     * Check if the node for the given numerical ID is named.
     *
     * @see Node#isNamed
     */
    public boolean isNamed(@Unsigned short symbol) {
        return ts_language_symbol_type(self, symbol) == TSSymbolTypeRegular();
    }

    /** Check if the node for the given numerical ID is visible. */
    public boolean isVisible(@Unsigned short symbol) {
        return ts_language_symbol_type(self, symbol) <= TSSymbolTypeAnonymous();
    }

    /**
     * Check if the node for the given numerical ID is a supertype.
     *
     * @since 0.24.0
     */
    public boolean isSupertype(@Unsigned short symbol) {
        return ts_language_symbol_type(self, symbol) == TSSymbolTypeSupertype();
    }

    /** Get the field name for the given numerical id. */
    public @Nullable String getFieldNameForId(@Unsigned short id) {
        var name = ts_language_field_name_for_id(self, id);
        return name.equals(MemorySegment.NULL) ? null : name.getString(0);
    }

    /** Get the numerical ID for the given field name. */
    public @Unsigned short getFieldIdForName(String name) {
        try (var arena = Arena.ofConfined()) {
            var segment = arena.allocateFrom(name);
            return ts_language_field_id_for_name(self, segment, name.length());
        }
    }

    /**
     * Get the next parse state.
     *
     * <p>{@snippet lang="java" :
     * short state = language.nextState(node.getParseState(), node.getGrammarSymbol());
     * }
     *
     * <p>Combine this with {@link #lookaheadIterator lookaheadIterator(state)}
     * to generate completion suggestions or valid symbols in {@index ERROR} nodes.
     */
    public @Unsigned short nextState(@Unsigned short state, @Unsigned short symbol) {
        return ts_language_next_state(self, state, symbol);
    }

    /**
     * Create a new lookahead iterator for the given parse state.
     *
     * @throws IllegalArgumentException If the state is invalid for this language.
     */
    public LookaheadIterator lookaheadIterator(@Unsigned short state) throws IllegalArgumentException {
        return new LookaheadIterator(self, state);
    }

    /**
     * Create a new query from a string containing one or more S-expression patterns.
     *
     * @throws QueryError If an error occurred while creating the query.
     * @deprecated Use the {@link Query#Query(Language, String) Query} constructor instead.
     */
    @Deprecated(since = "0.25.0")
    public Query query(String source) throws QueryError {
        return new Query(this, source);
    }

    /**
     * Get another reference to the language.
     *
     * @since 0.24.0
     */
    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public Language clone() {
        return new Language(ts_language_copy(self));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Language other)) return false;
        return self.equals(other.self);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(self.address());
    }

    @Override
    public String toString() {
        return "Language{id=0x%x, version=%d}".formatted(self.address(), version);
    }
}
