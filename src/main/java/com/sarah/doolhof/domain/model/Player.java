package com.sarah.doolhof.domain.model;

import com.sarah.doolhof.config.GameRules;
import com.sarah.doolhof.domain.maze.Cell;
import com.sarah.doolhof.domain.maze.Maze;

public final class Player extends Body {

    private double stamina = 1;
    private boolean exhausted;
    private boolean sprinting;
    private double distanceWalked;

    public Player(Cell start) {
        super(start);
    }

    public double stamina() {
        return stamina;
    }

    public boolean isExhausted() {
        return exhausted;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public double distanceWalked() {
        return distanceWalked;
    }

    void update(Maze maze, Input input, double dt) {
        sprinting = input.sprint() && input.isMoving() && !exhausted && stamina > 0;

        if (sprinting) {
            stamina = Math.max(0, stamina - GameRules.STAMINA_DRAIN * dt);
            exhausted = stamina == 0;
        } else {
            stamina = Math.min(1, stamina + GameRules.STAMINA_REGAIN * dt);
            if (stamina >= GameRules.STAMINA_RECOVERED_AT) {
                exhausted = false;
            }
        }

        if (!input.isMoving()) {
            return;
        }

        // Normalise, so walking diagonally is not faster than walking straight.
        double length = Math.hypot(input.moveX(), input.moveY());
        double distance = (sprinting ? GameRules.SPRINT_SPEED : GameRules.WALK_SPEED) * dt;
        double beforeX = x;
        double beforeY = y;

        moveAlongX(maze, input.moveX() / length * distance, distance);
        moveAlongY(maze, input.moveY() / length * distance, distance);

        distanceWalked += Math.hypot(x - beforeX, y - beforeY);
    }

    private void moveAlongX(Maze maze, double dx, double maxNudge) {
        if (dx == 0) {
            return;
        }
        if (!hitsWall(maze, x + dx, y)) {
            x += dx;
            return;
        }

        double centre = Math.floor(y) + 0.5;
        boolean corridorAhead = !hitsWall(maze, x + dx, centre);
        if (corridorAhead && y != centre) {
            y += Math.clamp(centre - y, -maxNudge, maxNudge);
        } else {
            snapToWallAlongX(dx);
        }
    }

    private void moveAlongY(Maze maze, double dy, double maxNudge) {
        if (dy == 0) {
            return;
        }
        if (!hitsWall(maze, x, y + dy)) {
            y += dy;
            return;
        }

        double centre = Math.floor(x) + 0.5;
        boolean corridorAhead = !hitsWall(maze, centre, y + dy);
        if (corridorAhead && x != centre) {
            x += Math.clamp(centre - x, -maxNudge, maxNudge);
        } else {
            snapToWallAlongY(dy);
        }
    }

    private void snapToWallAlongX(double dx) {
        x = dx > 0 ? Math.floor(x + HALF_SIZE - 1e-9) + 1 - HALF_SIZE : Math.floor(x - HALF_SIZE) + HALF_SIZE;
    }

    private void snapToWallAlongY(double dy) {
        y = dy > 0 ? Math.floor(y + HALF_SIZE - 1e-9) + 1 - HALF_SIZE : Math.floor(y - HALF_SIZE) + HALF_SIZE;
    }

    void reset(Cell start) {
        placeAt(start);
        stamina = 1;
        exhausted = false;
        sprinting = false;
    }
}
