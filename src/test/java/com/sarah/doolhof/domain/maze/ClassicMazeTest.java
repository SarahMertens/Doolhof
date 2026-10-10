package com.sarah.doolhof.domain.maze;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClassicMazeTest {

    private final Maze maze = ClassicMaze.create();

    @Test
    void hasTheLayoutOfTheFirstVersion() {
        String expected = String.join("\n",
            "#############################",
            "#     #   #       #       # #",
            "# ### # ### ##### # ####### #",
            "# #   #     #   #   #     # #",
            "# # ##### ### # ##### ##### #",
            "# #     #     #   #         #",
            "# ### # ##### ### # # # ### #",
            "#   # #   #   #   # # # #   #",
            "### ##### # ##### ### # # ###",
            "#   #     #     #   # # #   #",
            "# ### ### # ### ### # # ### #",
            "#   # #   #   # # #   # #   #",
            "### # ##### # # # ##### # ###",
            "#   #       # #     #   #   #",
            "#############################") + "\n";

        // Printed on failure, so a changed layout is easy to compare by eye.
        assertEquals(expected.replace(' ', '.'), maze.toAscii().replace(' ', '.'));
    }

    @Test
    void startAndDoorAreOpenAndConnected() {
        assertTrue(maze.isOpen(ClassicMaze.START));
        assertTrue(maze.isOpen(ClassicMaze.DOOR));
        assertFalse(Pathfinder.shortestPath(maze, ClassicMaze.START, ClassicMaze.DOOR).isEmpty());
    }
}
