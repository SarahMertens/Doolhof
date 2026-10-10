package com.sarah.doolhof.domain.maze;

public final class Visibility {

    private static final double WALL_DEPTH = 0.3;

    private Visibility() {
    }

    public record View(double[] xs, double[] ys, boolean[] seen) {
    }

    public static View compute(Maze maze, double x, double y, double radius, int rays) {
        double[] xs = new double[rays];
        double[] ys = new double[rays];
        boolean[] seen = new boolean[maze.tileCount()];

        for (int i = 0; i < rays; i++) {
            double angle = 2 * Math.PI * i / rays;
            double dirX = Math.cos(angle);
            double dirY = Math.sin(angle);

            double length = castRay(maze, x, y, dirX, dirY, radius, seen);
            xs[i] = x + dirX * length;
            ys[i] = y + dirY * length;
        }
        return new View(xs, ys, seen);
    }

    public static boolean hasLineOfSight(Maze maze, double x0, double y0, double x1, double y1) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double distance = Math.hypot(dx, dy);
        if (distance < 1e-9) {
            return true;
        }
        return distanceToWall(maze, x0, y0, dx / distance, dy / distance, distance) >= distance;
    }

    private static double castRay(Maze maze, double x, double y, double dirX, double dirY,
                                  double maxDistance, boolean[] seen) {
        int col = (int) Math.floor(x);
        int row = (int) Math.floor(y);
        mark(maze, seen, col, row);

        // Grid traversal (DDA): instead of taking small steps along the ray,
        // jump from one tile border to the next. tMaxX is the distance along
        // the ray to the next vertical border, tDeltaX the distance between
        // two vertical borders; likewise for Y.
        int stepCol = dirX > 0 ? 1 : -1;
        int stepRow = dirY > 0 ? 1 : -1;
        double tDeltaX = dirX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dirX);
        double tDeltaY = dirY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dirY);
        double tMaxX = dirX == 0 ? Double.POSITIVE_INFINITY
            : (dirX > 0 ? (col + 1 - x) : (x - col)) * tDeltaX;
        double tMaxY = dirY == 0 ? Double.POSITIVE_INFINITY
            : (dirY > 0 ? (row + 1 - y) : (y - row)) * tDeltaY;

        while (true) {
            double travelled;
            if (tMaxX < tMaxY) {
                travelled = tMaxX;
                tMaxX += tDeltaX;
                col += stepCol;
            } else {
                travelled = tMaxY;
                tMaxY += tDeltaY;
                row += stepRow;
            }

            if (travelled >= maxDistance) {
                return maxDistance;
            }

            if (maze.isWall(col, row)) {
                mark(maze, seen, col, row);
                return Math.min(travelled + WALL_DEPTH, maxDistance);
            }
            mark(maze, seen, col, row);
        }
    }

    private static double distanceToWall(Maze maze, double x, double y, double dirX, double dirY, double maxDistance) {
        // Same walk as castRay, without lighting the wall face.
        boolean[] ignored = new boolean[maze.tileCount()];
        double reached = castRay(maze, x, y, dirX, dirY, maxDistance + WALL_DEPTH, ignored);
        return reached >= maxDistance + WALL_DEPTH ? maxDistance : reached - WALL_DEPTH;
    }

    private static void mark(Maze maze, boolean[] seen, int col, int row) {
        if (maze.contains(col, row)) {
            seen[maze.index(col, row)] = true;
        }
    }
}
