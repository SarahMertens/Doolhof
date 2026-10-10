package com.sarah.doolhof.domain.maze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class MazeShifter {

    private MazeShifter() {
    }

    public static ShiftPlan plan(Maze maze, Random random, int changes, Set<Cell> protectedSet) {
        // Work on a copy, so every choice is checked against the choices made before it.
        Maze draft = maze.copy();
        List<Cell> opening = new ArrayList<>();
        List<Cell> closing = new ArrayList<>();

        for (Cell cell : shuffledInterior(draft, random)) {
            if (opening.size() == changes) {
                break;
            }
            if (draft.isWall(cell) && !protectedSet.contains(cell) && opensUsefulPassage(draft, cell)) {
                draft.setWall(cell, false);
                opening.add(cell);
            }
        }

        for (Cell cell : shuffledInterior(draft, random)) {
            if (closing.size() == changes) {
                break;
            }
            if (draft.isWall(cell) || protectedSet.contains(cell) || opening.contains(cell)) {
                continue;
            }

            // Try it, and undo it if any part of the maze becomes unreachable.
            draft.setWall(cell, true);
            if (Pathfinder.isFullyConnected(draft)) {
                closing.add(cell);
            } else {
                draft.setWall(cell, false);
            }
        }

        return new ShiftPlan(opening, closing);
    }

    public static ShiftPlan apply(Maze maze, ShiftPlan plan, Set<Cell> protectedSet) {
        List<Cell> opened = new ArrayList<>();
        List<Cell> closed = new ArrayList<>();

        for (Cell cell : plan.opening()) {
            if (maze.isWall(cell)) {
                maze.setWall(cell, false);
                opened.add(cell);
            }
        }

        for (Cell cell : plan.closing()) {
            if (maze.isWall(cell) || protectedSet.contains(cell)) {
                continue;
            }

            maze.setWall(cell, true);
            if (Pathfinder.isFullyConnected(maze)) {
                closed.add(cell);
            } else {
                maze.setWall(cell, false);
            }
        }

        return new ShiftPlan(opened, closed);
    }

    private static boolean opensUsefulPassage(Maze maze, Cell cell) {
        boolean connectsHorizontally = maze.isOpen(cell.step(Direction.WEST)) && maze.isOpen(cell.step(Direction.EAST));
        boolean connectsVertically = maze.isOpen(cell.step(Direction.NORTH)) && maze.isOpen(cell.step(Direction.SOUTH));

        return (connectsHorizontally || connectsVertically) && !wouldCreateOpenSquare(maze, cell);
    }

    private static boolean wouldCreateOpenSquare(Maze maze, Cell cell) {
        // The cell would be one corner of four possible 2 x 2 squares.
        for (int dCol = -1; dCol <= 0; dCol++) {
            for (int dRow = -1; dRow <= 0; dRow++) {
                int col = cell.col() + dCol;
                int row = cell.row() + dRow;

                boolean othersOpen = true;
                for (int c = col; c <= col + 1; c++) {
                    for (int r = row; r <= row + 1; r++) {
                        boolean isCellItself = c == cell.col() && r == cell.row();
                        if (!isCellItself && maze.isWall(c, r)) {
                            othersOpen = false;
                        }
                    }
                }
                if (othersOpen) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Cell> shuffledInterior(Maze maze, Random random) {
        List<Cell> cells = new ArrayList<>();
        for (int row = 1; row < maze.rows() - 1; row++) {
            for (int col = 1; col < maze.cols() - 1; col++) {
                cells.add(new Cell(col, row));
            }
        }
        Collections.shuffle(cells, random);
        return cells;
    }
}
