package com.sarah.doolhof.domain.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MazeGeneratorTest {

    private static long deadEnds(Maze maze) {
        return maze.openCells().stream().filter(maze::isDeadEnd).count();
    }

    @Test
    void theSameSeedGivesTheSameMaze() {
        Maze first = MazeGenerator.generate(21, 15, new Random(42), 0.3);
        Maze second = MazeGenerator.generate(21, 15, new Random(42), 0.3);
        Maze other = MazeGenerator.generate(21, 15, new Random(43), 0.3);

        assertEquals(first.toAscii(), second.toAscii());
        assertNotEquals(first.toAscii(), other.toAscii());
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 99, 2026, -7})
    void everyTileCanBeReachedAndTheBorderIsClosed(long seed) {
        Maze maze = MazeGenerator.generate(31, 21, new Random(seed), 0.25);

        assertTrue(Pathfinder.isFullyConnected(maze));
        for (int col = 0; col < maze.cols(); col++) {
            assertTrue(maze.isWall(col, 0) && maze.isWall(col, maze.rows() - 1));
        }
        for (int row = 0; row < maze.rows(); row++) {
            assertTrue(maze.isWall(0, row) && maze.isWall(maze.cols() - 1, row));
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 99, 2026})
    void withoutBraidingThereIsExactlyOneRouteBetweenAnyTwoTiles(long seed) {
        Maze maze = MazeGenerator.generate(25, 17, new Random(seed), 0);

        // A connected maze without loops is a tree: one corridor fewer than tiles.
        long tiles = maze.openCells().size();
        long corridors = maze.openCells().stream().mapToLong(cell -> maze.openNeighbours(cell).size()).sum() / 2;

        assertEquals(tiles - 1, corridors);
    }

    @Test
    void everyRoomIsCarved() {
        Maze maze = MazeGenerator.generate(21, 15, new Random(5), 0);

        for (int row = 1; row < maze.rows(); row += 2) {
            for (int col = 1; col < maze.cols(); col += 2) {
                assertTrue(maze.isOpen(col, row), "room at " + col + "," + row);
            }
        }
    }

    @Test
    void braidingRemovesDeadEnds() {
        long perfect = deadEnds(MazeGenerator.generate(41, 25, new Random(8), 0));
        long halfBraided = deadEnds(MazeGenerator.generate(41, 25, new Random(8), 0.5));
        long fullyBraided = deadEnds(MazeGenerator.generate(41, 25, new Random(8), 1));

        assertTrue(perfect > 20, "a perfect maze has plenty of dead ends, got " + perfect);
        assertTrue(halfBraided < perfect);
        assertTrue(fullyBraided < halfBraided);
    }

    @Test
    void rejectsEvenOrTinySizes() {
        assertThrows(IllegalArgumentException.class, () -> MazeGenerator.generate(20, 15, new Random(1), 0));
        assertThrows(IllegalArgumentException.class, () -> MazeGenerator.generate(21, 3, new Random(1), 0));
    }
}
