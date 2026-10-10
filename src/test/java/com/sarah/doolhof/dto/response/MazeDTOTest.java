package com.sarah.doolhof.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sarah.doolhof.common.TileMemory;
import org.junit.jupiter.api.Test;

class MazeDTOTest {

    /** Two tiles next to each other: a wall on the left, lit floor on the right. */
    private static MazeDTO twoTiles(boolean[] walls) {
        return new MazeDTO(2, 1, walls, new boolean[] {false, true},
            new TileMemory[] {TileMemory.UNKNOWN, TileMemory.FLOOR});
    }

    @Test
    void tilesAreReadByColumnAndRow() {
        MazeDTO maze = twoTiles(new boolean[] {true, false});

        assertTrue(maze.isWall(0, 0));
        assertTrue(maze.isOpen(1, 0));
        assertTrue(maze.isLit(1, 0));
        assertEquals(TileMemory.FLOOR, maze.memoryOf(1, 0));
        assertFalse(maze.isKnown(new CellDTO(0, 0)));
        assertTrue(maze.isKnown(new CellDTO(1, 0)));
    }

    @Test
    void outsideTheMazeIsDarkUnknownWall() {
        MazeDTO maze = twoTiles(new boolean[] {true, false});

        assertTrue(maze.isWall(2, 0));
        assertTrue(maze.isWall(0, -1));
        assertFalse(maze.isLit(5, 5));
        assertEquals(TileMemory.UNKNOWN, maze.memoryOf(-1, 0));
    }

    @Test
    void theDtoCannotBeChangedThroughItsArrays() {
        boolean[] walls = {true, false};
        MazeDTO maze = twoTiles(walls);

        walls[0] = false;
        maze.walls()[1] = true;

        assertTrue(maze.isWall(0, 0), "changing the array it was made from");
        assertTrue(maze.isOpen(1, 0), "changing the array it hands out");
    }

    @Test
    void aLightNeedsAnXAndAYForEveryCorner() {
        assertThrows(IllegalArgumentException.class, () -> new LightDTO(new double[2], new double[3]));

        double[] xs = {1, 2, 3};
        LightDTO light = new LightDTO(xs, new double[] {4, 5, 6});
        xs[0] = 99;

        assertEquals(3, light.cornerCount());
        assertEquals(1, light.x(0));
        assertEquals(6, light.y(2));
    }
}
