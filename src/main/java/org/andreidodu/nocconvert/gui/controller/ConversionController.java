package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.gui.controller.worker.ImageSearcherSwingWorker;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;

import javax.swing.*;
import java.nio.file.Path;
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
        List<FormatExtensionDTO> imageFormatList = imageConverterService.getAvailableWriteFormatList();
        conversionDTO.convertComponent().init("CONVERT to PNG", imageFormatList);
    }

    private void addConvertComponentEventListener() {
        conversionDTO.convertComponent().getMainActionButton()
                .addActionListener(e -> {

                    List<String> validationMessageList = conversionDTO.guiOrchestrator().getValidationMessageList();
                    if (!validationMessageList.isEmpty()) {
                        JOptionPane.showMessageDialog(conversionDTO.guiOrchestrator(), "Huston, we have some validation errors:\n" + String.join("\n", validationMessageList), "Validation Errors", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ImageSearcherSwingWorker worker = new ImageSearcherSwingWorker(conversionDTO.guiOrchestrator().getSourceDirectory(), conversionDTO.guiOrchestrator()::updateApplicationStatusLabel, this::onSearchComplete);
                    conversionDTO.applicationStatusLabel().setText("Looking for images in the Source directory...");
                    worker.execute();

                });
    }

    private void onSearchComplete(List<Path> paths) {

    }

}
