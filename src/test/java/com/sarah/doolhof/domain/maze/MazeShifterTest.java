package com.sarah.doolhof.domain.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MazeShifterTest {

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    void theMazeStaysFullyConnectedThroughManyShifts(long seed) {
        Random random = new Random(seed);
        Maze maze = MazeGenerator.generate(27, 19, random, 0.3);

        for (int shift = 0; shift < 25; shift++) {
            MazeShifter.plan(maze, random, 6, Set.of()).applyTo(maze);

            assertTrue(Pathfinder.isFullyConnected(maze), "cut off after shift " + shift + "\n" + maze.toAscii());
        }
    }

    @Test
    void aShiftReallyChangesTheMaze() {
        Random random = new Random(3);
        Maze maze = MazeGenerator.generate(27, 19, random, 0.3);
        String before = maze.toAscii();

        ShiftPlan plan = MazeShifter.plan(maze, random, 5, Set.of());

        assertEquals(before, maze.toAscii(), "planning alone must not touch the maze");
        assertEquals(5, plan.opening().size());
        assertEquals(5, plan.closing().size());

        plan.applyTo(maze);
        plan.opening().forEach(cell -> assertTrue(maze.isOpen(cell)));
        plan.closing().forEach(cell -> assertTrue(maze.isWall(cell)));
    }

    @Test
    void protectedTilesAndTheBorderAreNeverTouched() {
        Random random = new Random(11);
        Maze maze = MazeGenerator.generate(21, 15, random, 0.3);

        // Protect the left half; whatever changes must be on the right.
        Set<Cell> protectedTiles = new HashSet<>();
        for (int row = 0; row < maze.rows(); row++) {
            for (int col = 0; col <= 10; col++) {
                protectedTiles.add(new Cell(col, row));
            }
        }

        for (int shift = 0; shift < 20; shift++) {
            ShiftPlan plan = MazeShifter.plan(maze, random, 4, protectedTiles);

            for (Cell cell : plan.opening()) {
                assertFalse(protectedTiles.contains(cell));
                assertFalse(maze.isBorder(cell.col(), cell.row()));
            }
            for (Cell cell : plan.closing()) {
                assertFalse(protectedTiles.contains(cell));
            }
            plan.applyTo(maze);
        }
    }

    @Test
    void neverOpensATwoByTwoSquare() {
        Random random = new Random(21);
        Maze maze = MazeGenerator.generate(25, 17, random, 0);

        for (int shift = 0; shift < 30; shift++) {
            MazeShifter.plan(maze, random, 6, Set.of()).applyTo(maze);
        }

        for (int row = 1; row < maze.rows() - 1; row++) {
            for (int col = 1; col < maze.cols() - 1; col++) {
                boolean square = maze.isOpen(col, row) && maze.isOpen(col + 1, row)
                    && maze.isOpen(col, row + 1) && maze.isOpen(col + 1, row + 1);
                assertFalse(square, "open square at " + col + "," + row + "\n" + maze.toAscii());
            }
        }
    }

    @Test
    void givesAnEmptyPlanWhenNothingMayChange() {
        Maze maze = MazeGenerator.generate(11, 9, new Random(1), 0);
        Set<Cell> everything = new HashSet<>();
        for (int row = 0; row < maze.rows(); row++) {
            for (int col = 0; col < maze.cols(); col++) {
                everything.add(new Cell(col, row));
            }
        }

        assertTrue(MazeShifter.plan(maze, new Random(1), 5, everything).isEmpty());
    }
}
