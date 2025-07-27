package io.github.treesitter.jtreesitter;

import io.github.treesitter.jtreesitter.internal.*;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/** A match that corresponds to a certain pattern in a {@link Query}. */
@NullMarked
public record QueryMatch(@Unsigned int patternIndex, List<QueryCapture> captures) {
    /** Creates an instance of a QueryMatch record class. */
    public QueryMatch(@Unsigned int patternIndex, List<QueryCapture> captures) {
        this.patternIndex = patternIndex;
        this.captures = List.copyOf(captures);
    }

    static QueryMatch from(MemorySegment match, List<String> captureNames, Tree tree, SegmentAllocator allocator) {
        var count = Short.toUnsignedInt(TSQueryMatch.capture_count(match));
        var matchCaptures = TSQueryMatch.captures(match);
        var captureList = new ArrayList<QueryCapture>(count);
        for (int i = 0; i < count; ++i) {
            var capture = TSQueryCapture.asSlice(matchCaptures, i);
            var name = captureNames.get(TSQueryCapture.index(capture));
            var node = TSNode.allocate(allocator).copyFrom(TSQueryCapture.node(capture));
            captureList.add(new QueryCapture(name, new Node(node, tree)));
        }
        var patternIndex = TSQueryMatch.pattern_index(match);
        return new QueryMatch(patternIndex, captureList);
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
