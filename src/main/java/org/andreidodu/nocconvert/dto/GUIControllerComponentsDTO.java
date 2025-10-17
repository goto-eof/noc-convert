package org.andreidodu.nocconvert.dto;

import lombok.Builder;

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
                                         JComboBox<ImageFormatDTO> targetFileFormatComboBox,
                                         JPanel errorListPanel,
                                         JLabel fileFormatsJLabel) {
}
