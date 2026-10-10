package com.sarah.doolhof.domain.service;

import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.Pathfinder;
import com.sarah.doolhof.domain.model.Input;
import com.sarah.doolhof.domain.model.World;
import java.util.List;

public final class Autopilot {

    private Autopilot() {
    }

    public static Input nextInput(World world) {
        Cell here = world.player().cell();
        Cell goal = nearestGoal(world, here);

        List<Cell> path = Pathfinder.shortestPath(world.maze(), here, goal);
        Cell next = path.size() > 1 ? path.get(1) : goal;

        // Head for the centre of the next tile; the player's corner nudging does the rest.
        double dx = next.centerX() - world.player().x();
        double dy = next.centerY() - world.player().y();
        return new Input(direction(dx), direction(dy), false, false);
    }

    private static Cell nearestGoal(World world, Cell here) {
        if (world.keys().isEmpty()) {
            return world.door();
        }

        int[] distance = Pathfinder.distancesFrom(world.maze(), here);
        Cell nearest = world.keys().get(0);
        for (Cell key : world.keys()) {
            if (distance[world.maze().index(key)] < distance[world.maze().index(nearest)]) {
                nearest = key;
            }
        }
        return nearest;
    }

    private static double direction(double delta) {
        return Math.abs(delta) < 0.04 ? 0 : Math.signum(delta);
    }
}
