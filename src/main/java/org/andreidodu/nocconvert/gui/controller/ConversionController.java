package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.gui.controller.worker.ConversionWorker;
import org.andreidodu.nocconvert.gui.controller.worker.ImageSearcherSwingWorker;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class ConversionController {
    private static final Logger log = LogManager.getLogger(ConversionController.class);
    private final ConversionDTO conversionDTO;
    private final ImageConverterService imageConverterService;
    private final JLabel applicationStatusLabel;
    private final JLabel secondaryApplicationStatusLabel;
    private final JList<ConversionItemDTO> conversionItemList;

    public ConversionController(ConversionDTO conversionDTO) {
        this.conversionDTO = conversionDTO;
        this.imageConverterService = new ImageConverterServiceImpl();
        this.applicationStatusLabel = conversionDTO.applicationStatusLabel();
        this.secondaryApplicationStatusLabel = conversionDTO.secondaryApplicationStatusLabel();
        this.conversionItemList = conversionDTO.conversionFileList();
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

                    ImageSearcherSwingWorker worker = new ImageSearcherSwingWorker(conversionDTO.guiOrchestrator().getSourceDirectory(), this::updateApplicationStatus, this::onSearchComplete);
                    worker.execute();

                });
    }

    private void onSearchComplete(List<Path> paths) {
        endSearchForImagesStep(paths);
    }

    public void startSearchForImagesStep() {

    }

    public void endSearchForImagesStep(List<Path> paths) {
        conversionDTO.guiOrchestrator().onSearchStepFinish(conversionDTO.convertComponent().getSelectedItem().getExtension(), paths);
        SwingUtilities.invokeLater(() -> {
            String message = String.format("Search step done! %s processable images found.", paths.size());
            applicationStatusLabel.setText(message);
            secondaryApplicationStatusLabel.setText("Search done: " + paths.size() + " image(s) found");
            applicationStatusLabel.setVisible(true);
        });
    }

    public void updateApplicationStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            applicationStatusLabel.setText(text);
        });
    }

    public void startConversion(List<ConversionItemDTO> list) {
        log.info(list);
        new ConversionWorker(list, this::updateList).execute();
    }

    private void updateList(List<ConversionItemDTO> list) {
        conversionDTO.guiOrchestrator().updateList(list);
    }
}
