package org.andreidodu.nocconvert.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;

@Builder
public record PathSelectionDTO(TextfieldButtonComponent sourceComponent, TextfieldButtonComponent destinationComponent) {
}
