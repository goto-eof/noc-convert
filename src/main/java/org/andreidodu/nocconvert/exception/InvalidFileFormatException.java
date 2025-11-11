package org.andreidodu.nocconvert.exception;

public class InvalidFileFormatException extends RuntimeException {

    public InvalidFileFormatException() {
        super("", null, false, false);
    }

    public InvalidFileFormatException(String message) {
        super(message, null, false, false);
    }

    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, null, false, false);
    }
}
