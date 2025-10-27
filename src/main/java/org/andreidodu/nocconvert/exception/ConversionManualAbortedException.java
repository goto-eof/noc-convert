package org.andreidodu.nocconvert.exception;

public class ConversionManualAbortedException extends RuntimeException {
    public ConversionManualAbortedException() {
        super();
    }

    public ConversionManualAbortedException(String message) {
        super(message);
    }

    public ConversionManualAbortedException(String message, Throwable cause) {
        super(message, cause);
    }
}
