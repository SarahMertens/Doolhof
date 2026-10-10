package com.sarah.doolhof.domain.model;

import com.sarah.doolhof.common.HunterState;
import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.Maze;
import com.sarah.doolhof.domain.maze.Pathfinder;
import com.sarah.doolhof.domain.maze.Visibility;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public final class Hunter extends Body {

    private final Cell home;
    private final Deque<Cell> route = new ArrayDeque<>();

    private HunterState state = HunterState.PATROL;
    private Cell target;
    private double stateTimer;
    private double replanTimer;

    public Hunter(Cell home) {
        super(home);
        this.home = home;
    }

    public HunterState state() {
        return state;
    }

    public Cell home() {
        return home;
    }

    public boolean canSee(Maze maze, double targetX, double targetY) {
        return distanceTo(targetX, targetY) <= GameRules.HUNTER_SIGHT_RANGE
            && Visibility.hasLineOfSight(maze, x, y, targetX, targetY);
    }

    boolean update(Maze maze, Player player, boolean playerIsHidden, Random random, double dt) {
        if (state == HunterState.STUNNED) {
            stateTimer -= dt;
            if (stateTimer <= 0) {
                enter(HunterState.PATROL);
            }
            return false;
        }

        boolean sees = !playerIsHidden && canSee(maze, player.x(), player.y());
        boolean hears = !playerIsHidden && player.isSprinting() && distanceTo(player) <= GameRules.HUNTER_HEARING_RANGE;
        boolean noticed = false;

        if (sees) {
            noticed = state != HunterState.CHASE;
            state = HunterState.CHASE;
            target = player.cell();
        } else if (state == HunterState.CHASE) {
            // Lost sight: go and look where the player was last seen.
            enter(HunterState.SEARCH);
        } else if (hears && state == HunterState.PATROL) {
            noticed = true;
            enter(HunterState.SEARCH);
            target = player.cell();
            route.clear();
        }

        switch (state) {
            case CHASE -> {
                replanTimer -= dt;
                if (replanTimer <= 0 || route.isEmpty()) {
                    planRouteTo(maze, target);
                    replanTimer = GameRules.HUNTER_REPLAN_INTERVAL;
                }
                follow(maze, GameRules.HUNTER_CHASE_SPEED * dt);
            }
            case SEARCH -> {
                if (route.isEmpty() && !cell().equals(target)) {
                    planRouteTo(maze, target);
                }
                follow(maze, GameRules.HUNTER_SEARCH_SPEED * dt);
                if (route.isEmpty()) {
                    stateTimer -= dt;
                    if (stateTimer <= 0) {
                        enter(HunterState.PATROL);
                    }
                }
            }
            case PATROL -> {
                if (route.isEmpty()) {
                    planRouteTo(maze, randomDestination(maze, random));
                }
                follow(maze, GameRules.HUNTER_PATROL_SPEED * dt);
            }
            default -> { }
        }
        return noticed;
    }

    void stun(double seconds) {
        enter(HunterState.STUNNED);
        stateTimer = seconds;
    }

    void forgetRoute() {
        route.clear();
    }

    void reset() {
        placeAt(home);
        enter(HunterState.PATROL);
    }

    private void enter(HunterState next) {
        state = next;
        route.clear();
        stateTimer = next == HunterState.SEARCH ? GameRules.HUNTER_SEARCH_TIME : 0;
        replanTimer = 0;
        if (next == HunterState.PATROL) {
            target = null;
        }
    }

    private void planRouteTo(Maze maze, Cell destination) {
        route.clear();
        if (destination == null) {
            return;
        }

        List<Cell> path = Pathfinder.shortestPath(maze, cell(), destination);
        route.addAll(path);
    }

    private void follow(Maze maze, double distance) {
        while (distance > 0 && !route.isEmpty()) {
            Cell next = route.peek();
            if (maze.isWall(next)) {
                route.clear();
                return;
            }

            double dx = next.centerX() - x;
            double dy = next.centerY() - y;
            double gap = Math.hypot(dx, dy);

            if (gap <= distance) {
                x = next.centerX();
                y = next.centerY();
                distance -= gap;
                route.poll();
            } else {
                x += dx / gap * distance;
                y += dy / gap * distance;
                distance = 0;
            }
        }
    }

    private Cell randomDestination(Maze maze, Random random) {
        List<Cell> open = maze.openCells();
        Cell here = cell();

        // A few tries for a destination that is worth the walk.
        Cell choice = open.get(random.nextInt(open.size()));
        for (int attempt = 0; attempt < 8 && choice.manhattanDistanceTo(here) < 8; attempt++) {
            choice = open.get(random.nextInt(open.size()));
        }
        return choice;
    }
}
