package org.andreidodu.nocconvert.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversionItemDTO {

    private String fileName;
    private int fileSize;
    private int progressPercentage;
    private String targetFormat;
    private ConversionStatus status;

}
