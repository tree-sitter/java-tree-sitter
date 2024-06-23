package io.github.treesitter.jtreesitter;

import io.github.treesitter.jtreesitter.internal.TSRange;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import org.jspecify.annotations.NullMarked;

/**
 * A range of positions in a text document,
 * both in terms of bytes and of row-column points.
 */
@NullMarked
public record Range(Point startPoint, Point endPoint, @Unsigned int startByte, @Unsigned int endByte) {
    static final Range DEFAULT = new Range(Point.MIN, Point.MAX, 0, -1);

    /**
     * Creates an instance of a Range record class.
     *
     * @throws IllegalArgumentException If {@code startPoint > endPoint} or {@code startByte > endByte}.
     */
    public Range {
        if (startPoint.compareTo(endPoint) > 0) {
            throw new IllegalArgumentException("Invalid point range: %s to %s".formatted(startPoint, endPoint));
        }
        if (Integer.compareUnsigned(startByte, endByte) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Invalid byte range: %s to %s",
                    Integer.toUnsignedString(startByte), Integer.toUnsignedString(endByte)));
        }
    }

    static Range from(MemorySegment range) {
        int endByte = TSRange.end_byte(range), startByte = TSRange.start_byte(range);
        MemorySegment startPoint = TSRange.start_point(range), endPoint = TSRange.end_point(range);
        return new Range(Point.from(startPoint), Point.from(endPoint), startByte, endByte);
    }

    MemorySegment into(SegmentAllocator allocator) {
        var range = TSRange.allocate(allocator);
        TSRange.start_byte(range, startByte);
        TSRange.end_byte(range, endByte);
        TSRange.start_point(range, startPoint.into(allocator));
        TSRange.end_point(range, endPoint.into(allocator));
        return range;
    }

    @Override
    public String toString() {
        return String.format(
                "Range[startPoint=%s, endPoint=%s, startByte=%s, endByte=%s]",
                startPoint, endPoint, Integer.toUnsignedString(startByte), Integer.toUnsignedString(endByte));
    }
}
