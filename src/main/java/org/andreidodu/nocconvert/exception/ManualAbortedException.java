package org.andreidodu.nocconvert.exception;

public class ManualAbortedException extends RuntimeException {
    public ManualAbortedException() {
        super("", null, false, false);
    }

    public ManualAbortedException(String message) {
        super(message, null, false, false);
    }

    public ManualAbortedException(String message, Throwable cause) {
        super(message, null, false, false);
    }
}
