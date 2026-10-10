package com.sarah.doolhof.common;

public enum GameStatus {
    PLAYING,
    CAUGHT,
    WON,
    LOST;

    public boolean isOver() {
        return this == WON || this == LOST;
    }
}
