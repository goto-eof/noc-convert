package org.andreidodu.nocconvert.exception;

public class ConversionManualAbortedException extends ManualAbortedException {
    public ConversionManualAbortedException() {
        super();
    }

    public ConversionManualAbortedException(String message) {
        super(message);
    }

    public ConversionManualAbortedException(String message, Throwable cause) {
        super(message, null);
    }
}
