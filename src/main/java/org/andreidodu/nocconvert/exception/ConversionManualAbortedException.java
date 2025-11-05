package org.andreidodu.nocconvert.exception;

public class ConversionManualAbortedException extends RuntimeException {
    public ConversionManualAbortedException() {
        super("", null, false, false);
    }

    public ConversionManualAbortedException(String message) {
        super(message, null, false, false);
    }

    public ConversionManualAbortedException(String message, Throwable cause) {
        super(message, null, false, false);
    }
}
