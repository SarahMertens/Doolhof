package com.sarah.doolhof.exception;

public class NoGameRunningException extends DoolhofException {

    private static final long serialVersionUID = 1L;

    public NoGameRunningException() {
        super("No level has been started yet");
    }
}
