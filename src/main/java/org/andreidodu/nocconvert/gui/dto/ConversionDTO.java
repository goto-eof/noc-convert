package org.andreidodu.nocconvert.gui.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;

@Builder
public record ConversionDTO(GUIOrchestrator guiOrchestrator) {
}
