package com.sarah.doolhof.domain.model;

import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.Maze;

public abstract class Body {

    protected static final double HALF_SIZE = GameRules.BODY_HALF_SIZE;

    protected double x;
    protected double y;

    protected Body(Cell cell) {
        placeAt(cell);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public Cell cell() {
        return Cell.containing(x, y);
    }

    public final void placeAt(Cell cell) {
        x = cell.centerX();
        y = cell.centerY();
    }

    public double distanceTo(Body other) {
        return Math.hypot(other.x - x, other.y - y);
    }

    public double distanceTo(double otherX, double otherY) {
        return Math.hypot(otherX - x, otherY - y);
    }

    /** True when the square around (x, y) overlaps a wall. */
    protected static boolean hitsWall(Maze maze, double x, double y) {
        int left = (int) Math.floor(x - HALF_SIZE);
        int right = (int) Math.floor(x + HALF_SIZE - 1e-9);
        int top = (int) Math.floor(y - HALF_SIZE);
        int bottom = (int) Math.floor(y + HALF_SIZE - 1e-9);

        for (int row = top; row <= bottom; row++) {
            for (int col = left; col <= right; col++) {
                if (maze.isWall(col, row)) {
                    return true;
                }
            }
        }
        return false;
    }
}
