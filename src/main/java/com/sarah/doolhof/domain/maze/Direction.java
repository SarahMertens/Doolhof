package com.sarah.doolhof.domain.maze;

public enum Direction {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dCol;
    private final int dRow;

    Direction(int dCol, int dRow) {
        this.dCol = dCol;
        this.dRow = dRow;
    }

    public int dCol() {
        return dCol;
    }

    public int dRow() {
        return dRow;
    }
}
