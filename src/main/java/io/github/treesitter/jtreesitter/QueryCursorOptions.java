package io.github.treesitter.jtreesitter;

public class QueryCursorOptions {
    private int matchLimit = -1; // Default to unlimited
    private long timeoutMicros = -1; // Default to unlimited
    private int maxStartDepth = -1;
    private int startByte = -1;
    private int endByte = -1;
    private Point startPoint;
    private Point endPoint;


    public int getMatchLimit() {
        return matchLimit;
    }

    public void setMatchLimit(int matchLimit) throws IllegalArgumentException {
        if (matchLimit == 0) {
            throw new IllegalArgumentException("The match limit cannot equal 0");
        }
        this.matchLimit = matchLimit;
    }

    public long getTimeoutMicros() {
        return timeoutMicros;
    }

    public void setTimeoutMicros(long timeoutMicros) {
        this.timeoutMicros = timeoutMicros;
    }

    public int getMaxStartDepth() {
        return maxStartDepth;
    }

    public void setMaxStartDepth(int maxStartDepth) {
        this.maxStartDepth = maxStartDepth;
    }

    public int getStartByte() {
        return startByte;
    }

    public void setStartByte(int startByte) {
        this.startByte = startByte;
    }

    public int getEndByte() {
        return endByte;
    }

    public void setEndByte(int endByte) {
        this.endByte = endByte;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }
}
