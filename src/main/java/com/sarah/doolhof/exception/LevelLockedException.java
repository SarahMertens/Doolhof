package com.sarah.doolhof.exception;

public class LevelLockedException extends DoolhofException {

    private static final long serialVersionUID = 1L;

    public LevelLockedException(int number) {
        super("Level " + number + " is still locked");
    }
}
