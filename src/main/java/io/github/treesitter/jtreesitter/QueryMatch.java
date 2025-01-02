package io.github.treesitter.jtreesitter;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/** A match that corresponds to a certain pattern in the query. */
@NullMarked
public record QueryMatch(int matchId, @Unsigned int patternIndex, List<QueryCapture> captures) {
    /** Creates an instance of a QueryMatch record class. */
    public QueryMatch(@Unsigned int matchId, @Unsigned int patternIndex, List<QueryCapture> captures) {
        this.matchId = matchId;
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
                "QueryMatch[matchId=%s,patternIndex=%s, captures=%s]", Integer.toUnsignedString(matchId), Integer.toUnsignedString(patternIndex), captures);
    }
}
