package com.sarah.doolhof.domain.maze;

public final class ClassicMaze {

    public static final int COLS = 29;
    public static final int ROWS = 15;

    public static final Cell START = new Cell(1, 1);
    public static final Cell DOOR = new Cell(27, 13);

    private static final int[][] WALLS = {
        {1, 8, 2, 1}, {1, 12, 2, 1}, {2, 2, 1, 5}, {4, 6, 1, 8}, {3, 2, 2, 1}, {3, 6, 1, 1},
        {2, 10, 2, 1}, {4, 4, 5, 1}, {6, 1, 1, 3}, {8, 2, 3, 1}, {10, 1, 1, 1}, {5, 8, 4, 1},
        {6, 6, 1, 2}, {6, 12, 5, 1}, {6, 10, 3, 1}, {6, 11, 1, 1}, {10, 7, 1, 6}, {8, 6, 5, 1},
        {8, 5, 1, 1}, {12, 12, 1, 2}, {12, 10, 3, 1}, {14, 11, 1, 3}, {12, 8, 5, 1}, {14, 4, 1, 4},
        {16, 9, 1, 4}, {18, 12, 5, 1}, {20, 13, 1, 1}, {24, 6, 1, 8}, {22, 6, 1, 6}, {15, 6, 2, 1},
        {10, 4, 3, 1}, {12, 2, 5, 1}, {12, 3, 1, 1}, {16, 4, 5, 1}, {16, 3, 1, 1}, {18, 1, 1, 2},
        {18, 5, 1, 4}, {17, 10, 2, 1}, {18, 11, 1, 1}, {20, 6, 1, 5}, {19, 8, 1, 1}, {20, 2, 6, 1},
        {26, 1, 1, 4}, {20, 3, 1, 1}, {22, 4, 4, 1}, {25, 6, 2, 1}, {26, 8, 2, 1}, {25, 10, 2, 1},
        {26, 12, 2, 1},
    };

    private ClassicMaze() {
    }

    public static Maze create() {
        Maze maze = new Maze(COLS, ROWS);

        for (int row = 1; row < ROWS - 1; row++) {
            for (int col = 1; col < COLS - 1; col++) {
                maze.setWall(col, row, false);
            }
        }

        for (int[] wall : WALLS) {
            for (int row = wall[1]; row < wall[1] + wall[3]; row++) {
                for (int col = wall[0]; col < wall[0] + wall[2]; col++) {
                    maze.setWall(col, row, true);
                }
            }
        }
        return maze;
    }
}
