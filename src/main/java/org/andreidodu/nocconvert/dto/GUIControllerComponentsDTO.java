package org.andreidodu.nocconvert.dto;

import lombok.Builder;
import org.andreidodu.nocconvert.gui.components.ModernSplitButton;

import javax.swing.*;

@Builder
public record GUIControllerComponentsDTO(JFrame frame,
                                         JTextField sourceDirectoryTextField,
                                         JButton chooseSourceDirectoryButton,
                                         JList<String> sourceDirectoryFilesList,
                                         JTextField destinationDirectoryTextField,
                                         JButton chooseDestinationDirectoryButton,
                                         JButton convertButton,
                                         JList<String> errorsJList,
                                        ModernSplitButton targetFileFormatComboBox,
                                         JPanel errorListPanel,
                                         JLabel fileFormatsJLabel) {
}
