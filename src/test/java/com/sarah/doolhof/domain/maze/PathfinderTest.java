package com.sarah.doolhof.domain.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PathfinderTest {

    private final Maze maze = Maze.fromAscii(
        "#######",
        "#     #",
        "# ### #",
        "# #   #",
        "# # ###",
        "#   # #",
        "#######");

    @Test
    void countsStepsToEveryReachableTile() {
        int[] distance = Pathfinder.distancesFrom(maze, new Cell(1, 1));

        assertEquals(0, distance[maze.index(1, 1)]);
        assertEquals(4, distance[maze.index(5, 1)]);
        assertEquals(8, distance[maze.index(3, 3)], "the only way in is round the east side");
        assertEquals(Pathfinder.UNREACHABLE, distance[maze.index(5, 5)]);
        assertEquals(Pathfinder.UNREACHABLE, distance[maze.index(0, 0)]);
    }

    @Test
    void findsTheShortestRouteIncludingBothEnds() {
        List<Cell> path = Pathfinder.shortestPath(maze, new Cell(1, 1), new Cell(3, 5));

        assertEquals(new Cell(1, 1), path.get(0));
        assertEquals(new Cell(3, 5), path.get(path.size() - 1));
        assertEquals(7, path.size());
        for (int i = 1; i < path.size(); i++) {
            assertEquals(1, path.get(i - 1).manhattanDistanceTo(path.get(i)), "every step moves one tile");
            assertTrue(maze.isOpen(path.get(i)));
        }
    }

    @Test
    void aRouteToYourselfIsOneTile() {
        assertEquals(List.of(new Cell(1, 1)), Pathfinder.shortestPath(maze, new Cell(1, 1), new Cell(1, 1)));
    }

    @Test
    void thereIsNoRouteToAWallOrACutOffTile() {
        assertTrue(Pathfinder.shortestPath(maze, new Cell(1, 1), new Cell(5, 5)).isEmpty());
        assertTrue(Pathfinder.shortestPath(maze, new Cell(1, 1), new Cell(0, 0)).isEmpty());
        assertTrue(Pathfinder.shortestPath(maze, new Cell(0, 0), new Cell(1, 1)).isEmpty());
    }

    @Test
    void findsTheFarthestTile() {
        assertEquals(new Cell(3, 3), Pathfinder.farthestFrom(maze, new Cell(1, 1)));
    }

    @Test
    void noticesWhenPartOfTheMazeIsCutOff() {
        assertFalse(Pathfinder.isFullyConnected(maze));

        maze.setWall(5, 4, false);
        assertTrue(Pathfinder.isFullyConnected(maze));
    }
}
