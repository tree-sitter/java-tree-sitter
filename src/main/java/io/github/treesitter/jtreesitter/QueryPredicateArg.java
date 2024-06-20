package io.github.treesitter.jtreesitter;

import org.jspecify.annotations.NullMarked;

/** An argument to a {@link QueryPredicate}. */
@NullMarked
public sealed interface QueryPredicateArg permits QueryPredicateArg.Capture, QueryPredicateArg.Literal {
    /** The value of the argument. */
    String value();

    /** A capture argument ({@code @value}). */
    record Capture(String value) implements QueryPredicateArg {
        @Override
        public String toString() {
            return "@%s".formatted(value);
        }
    }

    /** A literal string argument ({@code "value"}). */
    record Literal(String value) implements QueryPredicateArg {
        @Override
        public String toString() {
            return "\"%s\"".formatted(value);
        }
    }
}
