package com.sarah.doolhof.domain.maze;

public record Cell(int col, int row) {

    public Cell step(Direction direction) {
        return new Cell(col + direction.dCol(), row + direction.dRow());
    }
    public int manhattanDistanceTo(Cell other) {
        return Math.abs(col - other.col) + Math.abs(row - other.row);
    }
    public double centerX() {
        return col + 0.5;
    }
    public double centerY() {
        return row + 0.5;
    }
    public static Cell containing(double x, double y) {
        return new Cell((int) Math.floor(x), (int) Math.floor(y));
    }
}
