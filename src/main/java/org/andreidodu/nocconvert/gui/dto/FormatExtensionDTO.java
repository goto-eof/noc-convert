package org.andreidodu.nocconvert.gui.dto;

import lombok.Getter;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.andreidodu.nocconvert.constants.ApplicationConfig.DEV_MODE;

@Getter
public class FormatExtensionDTO {
    private String format;
    private String extension;
    private String description;
    private final static AtomicInteger counter = new AtomicInteger(1);
    private int id = 0;

    public FormatExtensionDTO() {
    }

    public FormatExtensionDTO(String format, String extension) {
        this();
        this.format = format;
        this.extension = extension;
        this.description = buildDescriptionExt();
        this.id = counter.incrementAndGet();
    }

    public FormatExtensionDTO(String description, String format, String extension) {
        this(format, extension);
        this.description = description;
    }

    public static void reset() {
        counter.set(0);
    }

    public String getFullDescription() {
        if (DEV_MODE) {
            return String.format("%s - %s (*.%s, *.%s - %s)", id - FormatExtensionMapper.getFormatExtensionSTOList().size(), buildDescriptionExt(), extension.toLowerCase(), extension.toUpperCase(), format.toUpperCase());
        }
        return String.format("%s (*.%s, *.%s - %s)", buildDescriptionExt(), extension.toLowerCase(), extension.toUpperCase(), format.toUpperCase());
    }

    private String buildDescriptionExt() {
        return Optional.ofNullable(description)
                .orElseGet(() -> String.format("%s image", extension.toUpperCase()));
    }

    public String toString() {
        return getFullDescription();
    }
}
