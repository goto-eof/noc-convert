package org.andreidodu.nocconvert.gui.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;

@Builder
public record ConversionDTO(GUIOrchestrator guiOrchestrator, SplitButtonComponent convertComponent) {
}
