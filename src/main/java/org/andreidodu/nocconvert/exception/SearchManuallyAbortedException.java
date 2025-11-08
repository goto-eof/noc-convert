package org.andreidodu.nocconvert.exception;

public class SearchManuallyAbortedException extends ManualAbortedException {
    public SearchManuallyAbortedException() {
        super();
    }

    public SearchManuallyAbortedException(String message) {
        super(message);
    }

    public SearchManuallyAbortedException(String message, Throwable cause) {
        super(message, cause);
    }
}
