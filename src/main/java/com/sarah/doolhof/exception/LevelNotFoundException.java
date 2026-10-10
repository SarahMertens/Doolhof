package com.sarah.doolhof.exception;

public class LevelNotFoundException extends DoolhofException {

    private static final long serialVersionUID = 1L;

    public LevelNotFoundException(int number) {
        super("There is no level " + number);
    }
}
