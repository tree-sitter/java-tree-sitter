package io.github.treesitter.jtreesitter;

import io.github.treesitter.jtreesitter.internal.TSPoint;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

/**
 * A position in a text document in terms of rows and columns.
 *
 * @param row The zero-based row of the document.
 * @param column The zero-based column of the document.
 */
public record Point(@Unsigned int row, @Unsigned int column) implements Comparable<Point> {
    /** The minimum value a {@linkplain Point} can have. */
    public static final Point MIN = new Point(0, 0);

    /** The maximum value a {@linkplain Point} can have. */
    public static final Point MAX = new Point(-1, -1);

    static Point from(MemorySegment point) {
        return new Point(TSPoint.row(point), TSPoint.column(point));
    }

    MemorySegment into(SegmentAllocator allocator) {
        var point = TSPoint.allocate(allocator);
        TSPoint.row(point, row);
        TSPoint.column(point, column);
        return point;
    }

    @Override
    public int compareTo(Point other) {
        var rowDiff = Integer.compareUnsigned(row, other.row);
        if (rowDiff != 0) return rowDiff;
        return Integer.compareUnsigned(column, other.column);
    }

    @Override
    public String toString() {
        return "Point[row=%s, column=%s]".formatted(Integer.toUnsignedString(row), Integer.toUnsignedString(column));
    }
}
