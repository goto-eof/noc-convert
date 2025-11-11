package org.andreidodu.nocconvert.exception;

public class SourceFileConstraintsNotRespectedException extends RuntimeException {

    public SourceFileConstraintsNotRespectedException() {
        super("", null, false, false);
    }

    public SourceFileConstraintsNotRespectedException(String message) {
        super(message, null, false, false);
    }

    public SourceFileConstraintsNotRespectedException(String message, Throwable cause) {
        super(message, null, false, false);
    }
}
