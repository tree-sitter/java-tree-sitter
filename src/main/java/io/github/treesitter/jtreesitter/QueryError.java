package io.github.treesitter.jtreesitter;

import org.jspecify.annotations.NonNull;

/** Any error that occurred while instantiating a {@link Query}. */
public abstract sealed class QueryError extends IllegalArgumentException
        permits QueryError.Capture,
                QueryError.Field,
                QueryError.NodeType,
                QueryError.Structure,
                QueryError.Syntax,
                QueryError.Predicate {

    protected QueryError(@NonNull String message, Throwable cause) {
        super(message, cause);
    }

    protected QueryError(@NonNull String message) {
        super(message, null);
    }

    /** A query syntax error. */
    public static final class Syntax extends QueryError {
        Syntax() {
            super("Unexpected EOF");
        }

        Syntax(long row, long column) {
            super("Invalid syntax at row %d, column %d".formatted(row, column));
        }
    }

    /** A capture name error. */
    public static final class Capture extends QueryError {
        Capture(long row, long column, @NonNull CharSequence capture) {
            super("Invalid capture name at row %d, column %d: %s".formatted(row, column, capture));
        }
    }

    /** A field name error. */
    public static final class Field extends QueryError {
        Field(long row, long column, @NonNull CharSequence field) {
            super("Invalid field name at row %d, column %d: %s".formatted(row, column, field));
        }
    }

    /** A node type error. */
    public static final class NodeType extends QueryError {
        NodeType(long row, long column, @NonNull CharSequence type) {
            super("Invalid node type at row %d, column %d: %s".formatted(row, column, type));
        }
    }

    /** A pattern structure error. */
    public static final class Structure extends QueryError {
        Structure(long row, long column) {
            super("Impossible pattern at row %d, column %d".formatted(row, column));
        }
    }

    /** A query predicate error. */
    public static final class Predicate extends QueryError {
        Predicate(long row, @NonNull String details, Throwable cause) {
            super("Invalid predicate in pattern at row %d: %s".formatted(row, details), cause);
        }

        Predicate(long row, @NonNull String format, Object... args) {
            this(row, String.format(format, args), (Throwable) null);
        }
    }
}
