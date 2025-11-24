package org.andreidodu.nocconvert.gui.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;

import javax.swing.*;

@Builder
public record ConvertionStatusDTO(GUIOrchestrator guiOrchestrator,
                                  JScrollPane conversionFileListScrollPane,
                                  JPanel progressBarAndStatusPanel,
                                  JProgressBar progressBar,
                                  JProgressBar secondaryProgressBar,
                                  JList<ConversionItemDTO> conversionFileList,
                                  JPanel scrollPanePanel) {
}
