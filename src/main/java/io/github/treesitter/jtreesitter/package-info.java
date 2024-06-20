/**
 * Java bindings to the <a href="https://tree-sitter.github.io/tree-sitter/">tree-sitter</a> parsing library.
 *
 * <h2 id="requirements">Requirements</h2>
 *
 * <ul>
 * <li>
 *     JDK 22 (for the
 *     <a href="https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html">
 *         Foreign Function and Memory API
 *     </a>)
 * </li>
 * <li>Tree-sitter shared library</li>
 * <li>Generated bindings for languages</li>
 * <li>Shared libraries for languages</li>
 * </ul>
 *
 * <em>The shared libraries must be installed system-wide or in {@systemProperty java.library.path}</em>
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
 */
package io.github.treesitter.jtreesitter;
