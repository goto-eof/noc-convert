package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;

import javax.swing.*;
import java.util.List;

public class ConversionController {
    private final ConversionDTO conversionDTO;
    private final ImageConverterService imageConverterService;

    public ConversionController(ConversionDTO conversionDTO) {
        this.conversionDTO = conversionDTO;
        this.imageConverterService = new ImageConverterServiceImpl();
        initializeConvertComponent();
    }

    private void initializeConvertComponent() {
        populateConvertComponentDropdownMenu();
        addConvertComponentEventListener();
    }

    private void populateConvertComponentDropdownMenu() {
        List<FormatExtensionDTO> imageFormatList = imageConverterService.getAvailableFormatExtensionList();
        conversionDTO.convertComponent().init("CONVERT to PNG", imageFormatList);
    }

    private void addConvertComponentEventListener() {
        conversionDTO.convertComponent().getMainActionButton()
                .addActionListener(e -> {
                    JOptionPane.showMessageDialog(conversionDTO.guiOrchestrator(), "You selected: " + conversionDTO.convertComponent().getSelectedItem().getFormat(), "Action", JOptionPane.INFORMATION_MESSAGE);
                });
    }

}
