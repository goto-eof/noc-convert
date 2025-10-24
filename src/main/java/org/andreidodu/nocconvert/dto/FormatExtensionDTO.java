package org.andreidodu.nocconvert.dto;

import lombok.Getter;

import java.util.Optional;

@Getter
public class FormatExtensionDTO {
    private final String format;
    private final String extension;
    private String description;

    public FormatExtensionDTO(String format, String extension) {
        this.format = format;
        this.extension = extension;
    }

    public FormatExtensionDTO(String description, String format, String extension) {
        this(format, extension);
        this.description = description;
    }

    public String getFullDescription() {
        return String.format("%s (*.%s, *.%s - %s)", buildDescriptionExt(), extension.toLowerCase(), extension.toUpperCase(), format.toUpperCase());
    }

    private String buildDescriptionExt() {
        if (description != null) {
            return description + " image";
        }
        return Optional.ofNullable(description)
                .orElseGet(() -> String.format("%s image", extension.toUpperCase()));
    }

    public String toString() {
        return getFullDescription();
    }
}
