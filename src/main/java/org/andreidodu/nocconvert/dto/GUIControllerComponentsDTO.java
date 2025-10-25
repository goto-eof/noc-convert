package org.andreidodu.nocconvert.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.gui.components.ModernInputButtonPanel;
import org.andreidodu.nocconvert.gui.components.ModernSplitButton;

import javax.swing.*;

@Builder
public record GUIControllerComponentsDTO(JFrame frame,
                                         ModernInputButtonPanel sourceDirectoryTextField,
                                         JButton chooseSourceDirectoryButton,
                                         JList<ConversionItemDTO> sourceDirectoryFilesList,
                                         ModernInputButtonPanel destinationDirectoryTextField,
                                         JButton chooseDestinationDirectoryButton,
                                         JButton convertButton,
                                         JList<String> errorsJList,
                                        ModernSplitButton targetFileFormatComboBox,
                                         JPanel errorListPanel,
                                         JLabel fileFormatsJLabel) {
}
