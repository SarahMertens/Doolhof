package com.sarah.doolhof.domain.model;

public record Level(
    int number,
    String name,
    String hint,
    int cols,
    int rows,
    long seed,
    int keys,
    int hunters,
    int flares,
    double lightRadius,
    double shiftInterval,
    int shiftSize,
    boolean classic) {

    public boolean isDark() {
        return lightRadius > 0;
    }

    public boolean hasShiftingWalls() {
        return shiftInterval > 0;
    }

    public Level withoutHunters() {
        return new Level(number, name, hint, cols, rows, seed, keys, 0, 0, lightRadius, shiftInterval, shiftSize, classic);
    }
}
