package com.sarah.doolhof.dto.response;

import java.util.OptionalDouble;

public record LevelDTO(
    int number,
    String name,
    String hint,
    int keys,
    int hunters,
    int flares,
    boolean dark,
    double lightRadius,
    boolean shiftingWalls,
    double shiftInterval,
    boolean unlocked,
    OptionalDouble bestTime,
    boolean last) {
}
