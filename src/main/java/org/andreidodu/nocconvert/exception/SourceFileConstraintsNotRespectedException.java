package org.andreidodu.nocconvert.exception;

public class SourceFileConstraintsNotRespectedException extends RuntimeException {

    public SourceFileConstraintsNotRespectedException() {
        super();
    }

    public SourceFileConstraintsNotRespectedException(String message) {
        super(message);
    }

    public SourceFileConstraintsNotRespectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
