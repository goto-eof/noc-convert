package dto;

import lombok.Builder;

@Builder
public record ImageConversionResultDTO(String filename, boolean status) {
}
