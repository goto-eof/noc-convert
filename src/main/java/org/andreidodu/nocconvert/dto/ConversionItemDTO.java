package org.andreidodu.nocconvert.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Getter
@Setter
@Builder
public class ConversionItemDTO {

    private String fileName;
    private String fileSize;
    private float progressPercentage;
    private String targetFormat;
    private String targetExtension;
    private String errorMessage;
    private ConversionStatus status;
    private Path sourceFile;
    private Path destinationDirectory;
    private int index;
}
