package com.sarah.doolhof.domain.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Pathfinder {

    public static final int UNREACHABLE = -1;

    private Pathfinder() {
    }

    public static int[] distancesFrom(Maze maze, Cell start) {
        int[] distance = new int[maze.tileCount()];
        Arrays.fill(distance, UNREACHABLE);
        if (maze.isWall(start)) {
            return distance;
        }

        ArrayDeque<Cell> queue = new ArrayDeque<>();
        distance[maze.index(start)] = 0;
        queue.add(start);

        while (!queue.isEmpty()) {
            Cell current = queue.poll();
            int next = distance[maze.index(current)] + 1;

            for (Direction direction : Direction.values()) {
                Cell neighbour = current.step(direction);
                if (maze.isOpen(neighbour) && distance[maze.index(neighbour)] == UNREACHABLE) {
                    distance[maze.index(neighbour)] = next;
                    queue.add(neighbour);
                }
            }
        }
        return distance;
    }

    public static List<Cell> shortestPath(Maze maze, Cell from, Cell to) {
        // Searching backwards from the goal lets us walk the result forwards.
        int[] distance = distancesFrom(maze, to);
        if (maze.isWall(from) || distance[maze.index(from)] == UNREACHABLE) {
            return List.of();
        }

        List<Cell> path = new ArrayList<>();
        Cell current = from;
        path.add(current);

        while (!current.equals(to)) {
            int remaining = distance[maze.index(current)];
            for (Direction direction : Direction.values()) {
                Cell neighbour = current.step(direction);
                if (maze.isOpen(neighbour) && distance[maze.index(neighbour)] == remaining - 1) {
                    current = neighbour;
                    break;
                }
            }
            path.add(current);
        }
        return Collections.unmodifiableList(path);
    }

    public static Cell farthestFrom(Maze maze, Cell start) {
        int[] distance = distancesFrom(maze, start);
        Cell farthest = start;
        int best = 0;

        for (Cell cell : maze.openCells()) {
            if (distance[maze.index(cell)] > best) {
                best = distance[maze.index(cell)];
                farthest = cell;
            }
        }
        return farthest;
    }

    public static boolean isFullyConnected(Maze maze) {
        List<Cell> open = maze.openCells();
        if (open.isEmpty()) {
            return true;
        }

        int[] distance = distancesFrom(maze, open.get(0));
        for (Cell cell : open) {
            if (distance[maze.index(cell)] == UNREACHABLE) {
                return false;
            }
        }
        return true;
    }
}
