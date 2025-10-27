package org.andreidodu.nocconvert.dto;


import lombok.Builder;

@Builder
public record ConversionResultDTO(boolean success, String errorMessage, ConversionItemDTO conversionItemDTO) {
}
