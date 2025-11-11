package org.andreidodu.nocconvert.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;

import javax.swing.*;

@Builder
public record GUIControllerComponentsDTO(JFrame frame,
                                         TextfieldButtonComponent sourceDirectoryTextField,
                                         JButton chooseSourceDirectoryButton,
                                         JList<ConversionItemDTO> sourceDirectoryFilesList,
                                         TextfieldButtonComponent destinationDirectoryTextField,
                                         JButton chooseDestinationDirectoryButton,
                                         JButton convertButton,
                                         JList<String> errorsJList,
                                         SplitButtonComponent targetFileFormatComboBox,
                                         JPanel errorListPanel,
                                         JLabel fileFormatsJLabel) {
}
