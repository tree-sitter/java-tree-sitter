package io.github.treesitter.jtreesitter;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/** A match that corresponds to a certain pattern in the query. */
@NullMarked
public record QueryMatch(@Unsigned int patternIndex, List<QueryCapture> captures) {
    /** Creates an instance of a QueryMatch record class. */
    public QueryMatch(@Unsigned int patternIndex, List<QueryCapture> captures) {
        this.patternIndex = patternIndex;
        this.captures = List.copyOf(captures);
    }

    /** Find the nodes that are captured by the given capture name. */
    public List<Node> findNodes(String capture) {
        return captures.stream()
                .filter(c -> c.name().equals(capture))
                .map(QueryCapture::node)
                .toList();
    }

    @Override
    public String toString() {
        return String.format(
                "QueryMatch[patternIndex=%s, captures=%s]", Integer.toUnsignedString(patternIndex), captures);
    }
}
