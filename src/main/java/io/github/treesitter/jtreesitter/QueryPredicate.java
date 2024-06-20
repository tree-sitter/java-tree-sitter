package io.github.treesitter.jtreesitter;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * A query predicate that associates conditions (or arbitrary metadata) with a pattern.
 *
 * @see <a href="https://tree-sitter.github.io/tree-sitter/using-parsers#predicates">Predicates</a>
 */
@NullMarked
public sealed class QueryPredicate permits QueryPredicate.AnyOf, QueryPredicate.Eq, QueryPredicate.Match {
    private final String name;
    protected final List<QueryPredicateArg> args;

    protected QueryPredicate(String name, int argc) {
        this(name, new ArrayList<>(argc));
    }

    QueryPredicate(String name, List<QueryPredicateArg> args) {
        this.name = name;
        this.args = args;
    }

    /** Get the name of the predicate. */
    public String getName() {
        return name;
    }

    /** Get the arguments given to the predicate. */
    public List<QueryPredicateArg> getArgs() {
        return Collections.unmodifiableList(args);
    }

    boolean test(QueryMatch queryMatch) {
        return true;
    }

    @Override
    public String toString() {
        return "QueryPredicate{name=%s, args=%s}".formatted(name, args);
    }

    /**
     * Handles the following predicates:<br>
     * {@code #eq?}, {@code #not-eq?}, {@code #any-eq?}, {@code #any-not-eq?}
     */
    @NullMarked
    public static final class Eq extends QueryPredicate {
        private final String capture;
        private final String value;
        private final boolean isPositive;
        private final boolean isAny;
        private final boolean isCapture;

        static final Set<String> NAMES = Set.of("eq?", "not-eq?", "any-eq?", "any-not-eq?");

        Eq(String name, String capture, String value, boolean isCapture) {
            super(name, 2);
            this.capture = capture;
            this.value = value;
            this.isPositive = !name.contains("not-");
            this.isAny = name.startsWith("any-");
            this.isCapture = isCapture;

            args.add(new QueryPredicateArg.Capture(capture));
            if (isCapture) args.add(new QueryPredicateArg.Capture(value));
            else args.add(new QueryPredicateArg.Literal(value));
        }

        @Override
        boolean test(QueryMatch match) {
            return isCapture ? testCapture(match) : testLiteral(match);
        }

        private boolean testCapture(QueryMatch match) {
            var findNodes1 = match.findNodes(capture).stream();
            var findNodes2 = match.findNodes(value).stream();
            Predicate<Node> predicate =
                    n1 -> findNodes2.anyMatch(n2 -> Objects.equals(n1.getText(), n2.getText()) == isPositive);
            return isAny ? findNodes1.anyMatch(predicate) : findNodes1.allMatch(predicate);
        }

        private boolean testLiteral(QueryMatch match) {
            var findNodes1 = match.findNodes(capture);
            if (findNodes1.isEmpty()) return !isPositive;
            Predicate<Node> predicate = node -> {
                var text = Objects.requireNonNull(node.getText());
                return value.equals(text) == isPositive;
            };
            if (!isAny) return findNodes1.stream().allMatch(predicate);
            return findNodes1.stream().anyMatch(predicate);
        }
    }

    /**
     * Handles the following predicates:<br>
     * {@code #match?}, {@code #not-match?}, {@code #any-match?}, {@code #any-not-match?}
     */
    @NullMarked
    public static final class Match extends QueryPredicate {
        private final String capture;
        private final Pattern pattern;
        private final boolean isPositive;
        private final boolean isAny;

        static final Set<String> NAMES = Set.of("match?", "not-match?", "any-match?", "any-not-match?");

        Match(String name, String capture, Pattern pattern) {
            super(name, 2);
            this.capture = capture;
            this.pattern = pattern;
            this.isPositive = !name.contains("not-");
            this.isAny = name.startsWith("any-");

            args.add(new QueryPredicateArg.Capture(capture));
            args.add(new QueryPredicateArg.Literal(pattern.pattern()));
        }

        @Override
        boolean test(QueryMatch match) {
            var findNodes1 = match.findNodes(capture);
            if (findNodes1.isEmpty()) return !isPositive;
            Predicate<Node> predicate = node -> {
                var text = Objects.requireNonNull(node.getText());
                return pattern.matcher(text).hasMatch() == isPositive;
            };
            if (!isAny) return findNodes1.stream().allMatch(predicate);
            return findNodes1.stream().anyMatch(predicate);
        }
    }

    /**
     * Handles the following predicates:<br>
     * {@code #any-of?}, {@code #not-any-of?}
     */
    @NullMarked
    public static final class AnyOf extends QueryPredicate {
        private final String capture;
        private final List<String> values;
        private final boolean isPositive;

        static final Set<String> NAMES = Set.of("any-of?", "not-any-of?");

        AnyOf(String name, String capture, List<String> values) {
            super(name, values.size() + 1);
            this.capture = capture;
            this.values = List.copyOf(values);
            this.isPositive = name.equals("any-of?");

            args.add(new QueryPredicateArg.Capture(capture));
            for (var value : this.values) {
                args.add(new QueryPredicateArg.Literal(value));
            }
        }

        @Override
        boolean test(QueryMatch match) {
            return match.findNodes(capture).stream().noneMatch(node -> {
                var text = Objects.requireNonNull(node.getText());
                return values.contains(text) != isPositive;
            });
        }
    }
}
