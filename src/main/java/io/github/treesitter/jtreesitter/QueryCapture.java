package io.github.treesitter.jtreesitter;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link Node} that was captured with a certain capture name.
 *
 * @param name The name of the capture.
 * @param node The captured node.
 */
@NullMarked
public record QueryCapture(String name, Node node) {}
