package org.andreidodu.nocconvert.gui.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.andreidodu.nocconvert.gui.components.ButtonComponent;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;

import javax.swing.*;

@Builder
public record ConversionDTO(GUIOrchestrator guiOrchestrator,
                            SplitButtonComponent convertComponent,
                            JLabel applicationStatusLabel,
                            JLabel secondaryApplicationStatusLabel,
                            JList<ConversionItemDTO> conversionFileJList,
                            ButtonComponent openDestinationDirectoryButton) {
}
