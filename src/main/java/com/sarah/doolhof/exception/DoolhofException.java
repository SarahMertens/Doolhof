package com.sarah.doolhof.exception;

public abstract class DoolhofException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected DoolhofException(String message) {
        super(message);
    }

    protected DoolhofException(String message, Throwable cause) {
        super(message, cause);
    }
}
