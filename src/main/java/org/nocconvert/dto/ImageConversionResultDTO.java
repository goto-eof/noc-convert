package org.nocconvert.dto;

import lombok.Builder;

@Builder
public record ImageConversionResultDTO(String filename, boolean status) {
}
