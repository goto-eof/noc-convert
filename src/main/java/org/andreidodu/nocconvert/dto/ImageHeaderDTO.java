package org.andreidodu.nocconvert.dto;

import lombok.Builder;

@Builder
public record ImageHeaderDTO(String format, int width, int height, boolean isHeaderFound) {
}
