/**
 * Java bindings to the <a href="https://tree-sitter.github.io/tree-sitter/">tree-sitter</a> parsing library.
 *
 * <h2 id="requirements">Requirements</h2>
 *
 * <ul>
 * <li>
 *     JDK 23+ (for the
 *     <a href="https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html">
 *         Foreign Function and Memory API
 *     </a>)
 * </li>
 * <li>Tree-sitter shared library</li>
 * <li>Generated bindings for languages</li>
 * <li>Shared libraries for languages</li>
 * </ul>
 *
 * <h2 id="usage">Basic Usage</h2>
 *
 * {@snippet lang = java:
 * Language language = new Language(TreeSitterJava.language());
 * try (Parser parser = new Parser(language)) {
 *     try (Tree tree = parser.parse("void main() {}", InputEncoding.UTF_8).orElseThrow()) {
 *         Node rootNode = tree.getRootNode();
 *         assert rootNode.getType().equals("program");
 *         assert rootNode.getStartPoint().column() == 0;
 *         assert rootNode.getEndPoint().column() == 14;
 *     }
 * }
 *}
 *
 * <h2 id="libraries">Library Loading</h2>
 *
 * There are three ways to load the shared libraries:
 *
 * <ol>
 * <li>
 *     The libraries can be installed in the OS-specific library search path or in
 *     {@systemProperty java.library.path}. The search path can be amended using the
 *     {@code LD_LIBRARY_PATH} environment variable on Linux, {@code DYLD_LIBRARY_PATH}
 *     on macOS, or {@code PATH} on Windows. The libraries will be loaded automatically by
 *     {@link java.lang.foreign.SymbolLookup#libraryLookup(String, java.lang.foreign.Arena)
 *     SymbolLookup.libraryLookup(String, Arena)}.
 * </li>
 * <li>
 *     If the libraries are installed in {@systemProperty java.library.path} instead,
 *     they will be loaded automatically by {@link java.lang.foreign.SymbolLookup#loaderLookup()
 *     SymbolLookup.loaderLookup()}.
 * </li>
 * <li>
 *     The libraries can be loaded manually by registering a custom implementation of
 *     {@link io.github.treesitter.jtreesitter.NativeLibraryLookup NativeLibraryLookup}.
 *     This can be used, for example, to load libraries from inside a JAR file.
 * </li>
 * </ol>
 */
package io.github.treesitter.jtreesitter;
