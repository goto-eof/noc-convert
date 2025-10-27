package org.andreidodu.nocconvert.mapper;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;

public class ConversionItemDTOMapper {
    public  ConversionItemDTO clone(ConversionItemDTO conversionStatusDTO){
        return ConversionItemDTO.builder()
                .fileName(conversionStatusDTO.getFileName())
                .fileSize(conversionStatusDTO.getFileSize())
                .progressPercentage(conversionStatusDTO.getProgressPercentage())
                .targetFormat(conversionStatusDTO.getTargetFormat())
                .targetExtension(conversionStatusDTO.getTargetExtension())
                .errorMessage(conversionStatusDTO.getErrorMessage())
                .status(conversionStatusDTO.getStatus())
                .sourceFile(conversionStatusDTO.getSourceFile())
                .destinationDirectory(conversionStatusDTO.getDestinationDirectory())
                .index(conversionStatusDTO.getIndex())
                .build();
    }
}
