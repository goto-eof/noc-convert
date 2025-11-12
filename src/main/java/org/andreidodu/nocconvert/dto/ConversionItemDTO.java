package org.andreidodu.nocconvert.dto;

import lombok.*;

import java.nio.file.Path;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
public class ConversionItemDTO {

    private String fileName;
    private String fileSize;
    private volatile Float progressPercentage;
    private String targetFormat;
    private String targetExtension;
    private volatile String errorMessage;
    private volatile ConversionStatus status;
    private Path sourceFile;
    private Path tmpFile;
    private Path destinationDirectory;
    private Integer index;
    private Path tmpDirectory;

}
