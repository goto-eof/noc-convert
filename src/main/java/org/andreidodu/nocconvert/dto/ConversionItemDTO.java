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
    private Float progressPercentage;
    private String targetFormat;
    private String targetExtension;
    private String errorMessage;
    private ConversionStatus status;
    private Path sourceFile;
    private Path destinationDirectory;
    private Integer index;
    private boolean readOnly;

}
