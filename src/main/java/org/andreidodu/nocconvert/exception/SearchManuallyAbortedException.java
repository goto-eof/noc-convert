package org.andreidodu.nocconvert.exception;

public class SearchManuallyAbortedException extends RuntimeException {
    public SearchManuallyAbortedException() {
        super("", null, false, false);
    }

    public SearchManuallyAbortedException(String message) {
        super(message, null, false, false);
    }

    public SearchManuallyAbortedException(String message, Throwable cause) {
        super(message, null, false, false);
    }
}
