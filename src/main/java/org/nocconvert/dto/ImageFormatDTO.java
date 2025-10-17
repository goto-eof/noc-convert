package org.nocconvert.dto;

public record ImageFormatDTO(String description, String format) {

    public String toString() {
        return description;
    }
}
