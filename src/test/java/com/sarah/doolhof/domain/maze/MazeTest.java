package com.sarah.doolhof.domain.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MazeTest {

    private final Maze maze = Maze.fromAscii(
        "#####",
        "#   #",
        "# # #",
        "# # #",
        "#####");

    @Test
    void aNewMazeIsSolidWall() {
        assertTrue(new Maze(5, 5).openCells().isEmpty());
    }

    @Test
    void everythingOutsideTheGridIsWall() {
        assertTrue(maze.isWall(-1, 2));
        assertTrue(maze.isWall(2, 99));
        assertFalse(maze.contains(5, 0));
    }

    @Test
    void readsAndWritesTheSameText() {
        assertEquals("#####\n#   #\n# # #\n# # #\n#####\n", maze.toAscii());
        assertEquals(7, maze.openCells().size());
    }

    @Test
    void listsOnlyOpenNeighbours() {
        assertEquals(List.of(new Cell(2, 1), new Cell(1, 2)), maze.openNeighbours(new Cell(1, 1)));
    }

    @Test
    void aDeadEndHasExactlyOneWayOut() {
        assertTrue(maze.isDeadEnd(new Cell(1, 3)));
        assertFalse(maze.isDeadEnd(new Cell(1, 2)));
        assertFalse(maze.isDeadEnd(new Cell(2, 2)), "a wall is not a dead end");
    }

    @Test
    void aCopyDoesNotChangeTheOriginal() {
        Maze copy = maze.copy();
        copy.setWall(1, 1, true);

        assertTrue(maze.isOpen(1, 1));
    }

    @Test
    void rejectsImpossibleSizesAndTiles() {
        assertThrows(IllegalArgumentException.class, () -> new Maze(2, 9));
        assertThrows(IndexOutOfBoundsException.class, () -> maze.setWall(9, 9, true));
        assertThrows(IllegalArgumentException.class, () -> Maze.fromAscii("###", "#"));
    }

    @Test
    void cellsKnowTheirCentreAndTheTileAroundAPoint() {
        assertEquals(2.5, new Cell(2, 7).centerX());
        assertEquals(new Cell(3, 1), Cell.containing(3.99, 1.01));
        assertEquals(new Cell(-1, 0), Cell.containing(-0.2, 0.5));
        assertEquals(5, new Cell(1, 1).manhattanDistanceTo(new Cell(4, 3)));
    }
}
