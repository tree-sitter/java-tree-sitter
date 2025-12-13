package io.github.treesitter.jtreesitter;

import static io.github.treesitter.jtreesitter.internal.TreeSitter.*;

import io.github.treesitter.jtreesitter.internal.TSPoint;
import java.lang.foreign.Arena;
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

    /**
     * Edit the point to keep it in-sync with source code that has been edited.
     *
     * <p>This function updates the point's byte offset and row/column position based on an edit
     * operation. This is useful for editing points without requiring a tree or node instance.
     *
     * @return The new start byte of the point.
     * @since 0.26.0
     */
    public @Unsigned int edit(InputEdit edit) {
        try (var alloc = Arena.ofConfined()) {
            var start_byte = alloc.allocate(C_INT.byteSize(), C_INT.byteAlignment());
            ts_point_edit(into(alloc), start_byte, edit.into(alloc));
            return start_byte.get(C_INT, 0);
        }
    }

    @Override
    public String toString() {
        return "Point[row=%s, column=%s]".formatted(Integer.toUnsignedString(row), Integer.toUnsignedString(column));
    }
}
