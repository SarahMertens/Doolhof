package com.sarah.doolhof.domain.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VisibilityTest {

    private final Maze maze = Maze.fromAscii(
        "#########",
        "#       #",
        "# ##### #",
        "# #   # #",
        "# ##### #",
        "#       #",
        "#########");

    @Test
    void seesAlongAStraightCorridor() {
        assertTrue(Visibility.hasLineOfSight(maze, 1.5, 1.5, 7.5, 1.5));
        assertTrue(Visibility.hasLineOfSight(maze, 7.5, 1.5, 7.5, 5.5));
    }

    @Test
    void aWallBlocksTheView() {
        assertFalse(Visibility.hasLineOfSight(maze, 1.5, 1.5, 7.5, 5.5), "diagonal through the block");
        assertFalse(Visibility.hasLineOfSight(maze, 4.5, 1.5, 4.5, 3.5), "into the closed room");
        assertFalse(Visibility.hasLineOfSight(maze, 1.5, 3.5, 3.5, 3.5));
    }

    @Test
    void aPointSeesItself() {
        assertTrue(Visibility.hasLineOfSight(maze, 1.5, 1.5, 1.5, 1.5));
    }

    @Test
    void lineOfSightWorksBothWays() {
        double[][] points = {{1.5, 1.5}, {7.5, 1.5}, {1.5, 5.5}, {4.5, 3.5}, {7.2, 5.8}, {3.5, 5.5}};

        for (double[] a : points) {
            for (double[] b : points) {
                assertEquals(
                    Visibility.hasLineOfSight(maze, a[0], a[1], b[0], b[1]),
                    Visibility.hasLineOfSight(maze, b[0], b[1], a[0], a[1]),
                    "from " + a[0] + "," + a[1] + " to " + b[0] + "," + b[1]);
            }
        }
    }

    @Test
    void theLitAreaStaysWithinTheRadius() {
        Visibility.View view = Visibility.compute(maze, 1.5, 1.5, 3, 360);

        assertEquals(360, view.xs().length);
        for (int i = 0; i < view.xs().length; i++) {
            assertTrue(Math.hypot(view.xs()[i] - 1.5, view.ys()[i] - 1.5) <= 3 + 1e-9);
        }
    }

    @Test
    void marksTheTilesInViewAndTheWallsThatStopIt() {
        Visibility.View view = Visibility.compute(maze, 1.5, 1.5, 10, 720);

        assertTrue(view.seen()[maze.index(1, 1)], "own tile");
        assertTrue(view.seen()[maze.index(7, 1)], "end of the corridor");
        assertTrue(view.seen()[maze.index(1, 5)], "end of the other corridor");
        assertTrue(view.seen()[maze.index(8, 1)], "the wall that stops the view");
        assertTrue(view.seen()[maze.index(2, 2)], "the corner block");

        assertFalse(view.seen()[maze.index(4, 3)], "the closed room");
        assertFalse(view.seen()[maze.index(7, 5)], "round two corners");
    }

    @Test
    void aShortRadiusLeavesFarTilesUnseen() {
        Visibility.View view = Visibility.compute(maze, 1.5, 1.5, 2, 360);

        assertTrue(view.seen()[maze.index(3, 1)]);
        assertFalse(view.seen()[maze.index(7, 1)]);
    }
}
