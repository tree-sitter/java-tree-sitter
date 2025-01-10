package io.github.treesitter.jtreesitter;

import org.jspecify.annotations.Nullable;

/**
 * Configuration for creating a {@link QueryCursor}.
 *
 * @see Query#execute(Node, QueryCursorConfig)
 */
public class QueryCursorConfig {
    private int matchLimit = -1; // Default to unlimited
    private long timeoutMicros = 0; // Default to unlimited
    private int maxStartDepth = -1;
    private int startByte = -1;
    private int endByte = -1;
    private Point startPoint;
    private Point endPoint;

    /**
     * Get the maximum number of in-progress matches.
     *
     * @apiNote Defaults to {@code -1} (unlimited).
     */
    public int getMatchLimit() {
        return matchLimit;
    }

    /**
     * Set the maximum number of in-progress matches.
     *
     * @apiNote Defaults to {@code -1} (unlimited).
     */
    public void setMatchLimit(int matchLimit) throws IllegalArgumentException {
        if (matchLimit == 0) {
            throw new IllegalArgumentException("The match limit cannot equal 0");
        }
        this.matchLimit = matchLimit;
    }

    /**
     * Get the maximum duration in microseconds that query
     * execution should be allowed to take before halting.
     *
     * @return the timeout in microseconds
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public long getTimeoutMicros() {
        return timeoutMicros;
    }

    /**
     * Set the maximum duration in microseconds that query execution
     * should be allowed to take before halting.
     *
     * @param timeoutMicros the timeout in microseconds
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public void setTimeoutMicros(long timeoutMicros) {
        this.timeoutMicros = timeoutMicros;
    }

    /**
     * Get the maximum start depth for the query cursor
     */
    public int getMaxStartDepth() {
        return maxStartDepth;
    }

    /**
     * Set the maximum start depth for the query cursor.
     *
     * <p>This prevents cursors from exploring children nodes at a certain depth.
     * <br>Note that if a pattern includes many children, then they will still be checked.
     */
    public void setMaxStartDepth(int maxStartDepth) {
        this.maxStartDepth = maxStartDepth;
    }

    /**
     * Set the range of bytes in which the query will be executed.
     */
    public void setByteRange(@Unsigned int startByte, @Unsigned int endByte) {
        this.startByte = startByte;
        this.endByte = endByte;
    }

    /**
     * Get the start byte of the range of bytes in which the query will be executed or -1 if not set.
     */
    public int getStartByte() {
        return startByte;
    }

    /**
     * Get the end byte of the range of bytes in which the query will be executed or -1 if not set.
     */
    public int getEndByte() {
        return endByte;
    }

    /**
     * Set the range of points in which the query will be executed.
     */
    public void setPointRange(Point startPoint, Point endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    /**
     * Get the start point of the range of points in which the query will be executed or {@code null} if not set.
     */
    public @Nullable Point getStartPoint() {
        return startPoint;
    }

    /**
     * Get the end point of the range of points in which the query will be executed or {@code null} if not set.
     */
    public @Nullable Point getEndPoint() {
        return endPoint;
    }
}
