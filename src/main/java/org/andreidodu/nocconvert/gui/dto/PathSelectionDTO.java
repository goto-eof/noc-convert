package org.andreidodu.nocconvert.gui.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;

@Builder
public record PathSelectionDTO(GUIOrchestrator guiOrchestrator, TextfieldButtonComponent sourceComponent,
                               TextfieldButtonComponent destinationComponent) {
}
