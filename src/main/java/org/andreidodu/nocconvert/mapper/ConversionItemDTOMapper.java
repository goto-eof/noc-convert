package org.andreidodu.nocconvert.mapper;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;

public class ConversionItemDTOMapper {
    public  ConversionItemDTO clone(ConversionItemDTO conversionItemDTO){
        return ConversionItemDTO.builder()
                .fileName(conversionItemDTO.getFileName())
                .fileSize(conversionItemDTO.getFileSize())
                .progressPercentage(conversionItemDTO.getProgressPercentage())
                .targetFormat(conversionItemDTO.getTargetFormat())
                .targetExtension(conversionItemDTO.getTargetExtension())
                .errorMessage(conversionItemDTO.getErrorMessage())
                .status(conversionItemDTO.getStatus())
                .sourceFile(conversionItemDTO.getSourceFile())
                .destinationDirectory(conversionItemDTO.getDestinationDirectory())
                .index(conversionItemDTO.getIndex())
                .build();
    }
}
