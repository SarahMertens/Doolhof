package com.sarah.doolhof.domain.model;

import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.ClassicMaze;
import com.sarah.doolhof.domain.maze.Maze;
import com.sarah.doolhof.domain.maze.MazeGenerator;
import com.sarah.doolhof.domain.maze.Pathfinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WorldFactory {

    private WorldFactory() {
    }

    public static World create(Level level) {
        Random random = new Random(level.seed());

        if (level.classic()) {
            return new World(level, ClassicMaze.create(), ClassicMaze.START, ClassicMaze.DOOR,
                List.of(), List.of(), random);
        }

        Maze maze = MazeGenerator.generate(level.cols(), level.rows(), random, GameRules.MAZE_BRAID);
        Cell start = new Cell(1, 1);
        Cell door = Pathfinder.farthestFrom(maze, start);

        List<Cell> keys = spreadOut(maze, level.keys(), List.of(start, door), 0);

        int longestRoute = Pathfinder.distancesFrom(maze, start)[maze.index(door)];
        List<Cell> hunters = spreadOut(maze, level.hunters(), List.of(start),
            (int) (longestRoute * GameRules.HUNTER_MIN_START_DISTANCE));

        return new World(level, maze, start, door, keys, hunters, random);
    }

    static List<Cell> spreadOut(Maze maze, int count, List<Cell> taken, int minFromFirst) {
        List<Cell> chosen = new ArrayList<>();
        List<int[]> distanceMaps = new ArrayList<>();
        taken.forEach(cell -> distanceMaps.add(Pathfinder.distancesFrom(maze, cell)));

        for (int i = 0; i < count; i++) {
            Cell best = null;
            int bestScore = -1;

            for (Cell candidate : maze.openCells()) {
                int index = maze.index(candidate);
                if (distanceMaps.get(0)[index] < minFromFirst) {
                    continue;
                }

                int nearest = Integer.MAX_VALUE;
                for (int[] distances : distanceMaps) {
                    nearest = Math.min(nearest, distances[index]);
                }

                if (nearest > bestScore) {
                    bestScore = nearest;
                    best = candidate;
                }
            }

            if (best == null) {
                break;
            }
            chosen.add(best);
            distanceMaps.add(Pathfinder.distancesFrom(maze, best));
        }
        return chosen;
    }
}
