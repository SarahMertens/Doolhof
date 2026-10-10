package com.sarah.doolhof.domain.maze;

import java.util.ArrayList;
import java.util.List;

public final class Maze {

    private final int cols;
    private final int rows;
    private final boolean[] wall;

    public Maze(int cols, int rows) {
        if (cols < 3 || rows < 3) {
            throw new IllegalArgumentException("A maze needs at least 3 x 3 tiles, got " + cols + " x " + rows);
        }
        this.cols = cols;
        this.rows = rows;
        this.wall = new boolean[cols * rows];
        java.util.Arrays.fill(wall, true);
    }

    private Maze(Maze other) {
        this.cols = other.cols;
        this.rows = other.rows;
        this.wall = other.wall.clone();
    }

    public Maze copy() {
        return new Maze(this);
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    public boolean contains(int col, int row) {
        return col >= 0 && row >= 0 && col < cols && row < rows;
    }

    public boolean isWall(int col, int row) {
        return !contains(col, row) || wall[index(col, row)];
    }

    public boolean isWall(Cell cell) {
        return isWall(cell.col(), cell.row());
    }

    public boolean isOpen(int col, int row) {
        return !isWall(col, row);
    }

    public boolean isOpen(Cell cell) {
        return !isWall(cell);
    }

    /** True for the outer ring of tiles, which always stays wall. */
    public boolean isBorder(int col, int row) {
        return col == 0 || row == 0 || col == cols - 1 || row == rows - 1;
    }

    public void setWall(int col, int row, boolean isWall) {
        if (!contains(col, row)) {
            throw new IndexOutOfBoundsException("Tile (" + col + ", " + row + ") is outside the maze");
        }
        wall[index(col, row)] = isWall;
    }

    public void setWall(Cell cell, boolean isWall) {
        setWall(cell.col(), cell.row(), isWall);
    }

    /** Position of a tile in a flat array of {@code cols * rows} elements. */
    public int index(int col, int row) {
        return row * cols + col;
    }

    public int index(Cell cell) {
        return index(cell.col(), cell.row());
    }

    public int tileCount() {
        return wall.length;
    }

    public List<Cell> openCells() {
        List<Cell> cells = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (isOpen(col, row)) {
                    cells.add(new Cell(col, row));
                }
            }
        }
        return cells;
    }

    /** The open tiles directly north, east, south and west of a tile. */
    public List<Cell> openNeighbours(Cell cell) {
        List<Cell> neighbours = new ArrayList<>(4);
        for (Direction direction : Direction.values()) {
            Cell next = cell.step(direction);
            if (isOpen(next)) {
                neighbours.add(next);
            }
        }
        return neighbours;
    }

    /** A dead end is an open tile with exactly one way out. */
    public boolean isDeadEnd(Cell cell) {
        return isOpen(cell) && openNeighbours(cell).size() == 1;
    }

    /** Draws the maze as text: {@code #} for wall, a space for floor. Handy in tests. */
    public String toAscii() {
        StringBuilder text = new StringBuilder((cols + 1) * rows);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                text.append(isWall(col, row) ? '#' : ' ');
            }
            text.append('\n');
        }
        return text.toString();
    }

    /** Reads a maze from text, the inverse of {@link #toAscii()}. Any character but {@code #} is floor. */
    public static Maze fromAscii(String... lines) {
        int rows = lines.length;
        int cols = lines[0].length();
        Maze maze = new Maze(cols, rows);

        for (int row = 0; row < rows; row++) {
            if (lines[row].length() != cols) {
                throw new IllegalArgumentException("Line " + row + " is not " + cols + " characters long");
            }
            for (int col = 0; col < cols; col++) {
                maze.setWall(col, row, lines[row].charAt(col) == '#');
            }
        }
        return maze;
    }
}
