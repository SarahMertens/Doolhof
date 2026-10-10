package com.sarah.doolhof.dto.response;

import com.sarah.doolhof.common.GameEvent;
import com.sarah.doolhof.common.GameStatus;
import java.util.List;

public record GameDTO(
    LevelDTO level,
    GameStatus status,
    MazeDTO maze,
    LightDTO light,
    PlayerDTO player,
    List<HunterDTO> hunters,
    List<CellDTO> keys,
    int keysCollected,
    int totalKeys,
    CellDTO door,
    boolean doorOpen,
    int lives,
    int maxLives,
    int flares,
    double elapsed,
    boolean flareActive,
    double flareStrength,
    boolean shiftPending,
    WallShiftDTO pendingShift,
    WallShiftDTO lastShift,
    double shiftCountdown,
    boolean chased,
    double nearestHunterDistance,
    List<GameEvent> events,
    boolean newRecord) {

    public GameDTO {
        hunters = List.copyOf(hunters);
        keys = List.copyOf(keys);
        events = List.copyOf(events);
    }
}
