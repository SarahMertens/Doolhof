package com.sarah.doolhof.domain.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class MazeGenerator {

    private MazeGenerator() {
    }

    public static Maze generate(int cols, int rows, Random random, double braid) {
        if (cols < 5 || rows < 5 || cols % 2 == 0 || rows % 2 == 0) {
            throw new IllegalArgumentException("Size must be odd and at least 5 x 5, got " + cols + " x " + rows);
        }

        Maze maze = new Maze(cols, rows);
        carve(maze, random);
        braid(maze, random, braid);
        return maze;
    }

    private static void carve(Maze maze, Random random) {
        // Rooms sit on odd coordinates; the tiles between them are the walls we may break.
        ArrayDeque<Cell> trail = new ArrayDeque<>();
        Cell start = new Cell(1, 1);
        maze.setWall(start, false);
        trail.push(start);

        List<Direction> directions = new ArrayList<>(List.of(Direction.values()));

        while (!trail.isEmpty()) {
            Cell current = trail.peek();
            Collections.shuffle(directions, random);

            boolean moved = false;
            for (Direction direction : directions) {
                Cell between = current.step(direction);
                Cell room = between.step(direction);

                if (isInterior(maze, room) && maze.isWall(room)) {
                    maze.setWall(between, false);
                    maze.setWall(room, false);
                    trail.push(room);
                    moved = true;
                    break;
                }
            }

            if (!moved) {
                trail.pop();
            }
        }
    }

    private static void braid(Maze maze, Random random, double share) {
        for (Cell cell : maze.openCells()) {
            if (!maze.isDeadEnd(cell) || random.nextDouble() >= share) {
                continue;
            }

            // Break through one of the closed sides into the room behind it.
            List<Direction> options = new ArrayList<>();
            for (Direction direction : Direction.values()) {
                Cell between = cell.step(direction);
                Cell beyond = between.step(direction);
                if (maze.isWall(between) && isInterior(maze, between) && maze.isOpen(beyond)) {
                    options.add(direction);
                }
            }

            if (!options.isEmpty()) {
                maze.setWall(cell.step(options.get(random.nextInt(options.size()))), false);
            }
        }
    }

    private static boolean isInterior(Maze maze, Cell cell) {
        return maze.contains(cell.col(), cell.row()) && !maze.isBorder(cell.col(), cell.row());
    }
}
