package org.andreidodu.nocconvert.gui.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;
import org.andreidodu.nocconvert.gui.components.TextfieldDoubleButtonComponent;

@Builder
public record PathSelectionDTO(GUIOrchestrator guiOrchestrator, TextfieldDoubleButtonComponent sourceComponent,
                               TextfieldDoubleButtonComponent destinationComponent) {
}
