package org.andreidodu.nocconvert.dto;

import java.awt.*;

public record ConversionStatus(String name, Color displayColor, String status) {
    public final static String QUEUED = "QUEUED";
    public final static String COMPLETED = "COMPLETED";
    public final static String FAILED = "FAILED";
}
