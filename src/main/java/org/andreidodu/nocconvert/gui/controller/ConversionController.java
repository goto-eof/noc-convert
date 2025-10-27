package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class ConversionController {
    private static final Logger log = LogManager.getLogger(ConversionController.class);
    private final ConversionDTO conversionDTO;
    private final ImageConverterService imageConverterService;
    private final JLabel applicationStatusLabel;
    private final JLabel secondaryApplicationStatusLabel;

    public ConversionController(ConversionDTO conversionDTO) {
        this.conversionDTO = conversionDTO;
        this.imageConverterService = new ImageConverterServiceImpl();
        this.applicationStatusLabel = conversionDTO.applicationStatusLabel();
        this.secondaryApplicationStatusLabel = conversionDTO.secondaryApplicationStatusLabel();
        initializeConvertComponent();
    }

    private void initializeConvertComponent() {
        populateConvertComponentDropdownMenu();
        addConvertComponentEventListener();
    }

    private void populateConvertComponentDropdownMenu() {
        List<FormatExtensionDTO> imageFormatList = imageConverterService.getAvailableWriteFormatList();
        FormatExtensionDTO preferredFormat = imageFormatList.stream().filter(format -> "webp".equals(format.getFormat())).findFirst().orElse(imageFormatList.getLast());
        conversionDTO.convertComponent().init("CONVERT to PNG", imageFormatList, preferredFormat);
    }

    private void addConvertComponentEventListener() {
        conversionDTO.convertComponent().getMainActionButton()
                .addActionListener(e -> {

                    List<String> validationMessageList = conversionDTO.guiOrchestrator().getValidationMessageList();
                    if (!validationMessageList.isEmpty()) {
                        JOptionPane.showMessageDialog(conversionDTO.guiOrchestrator(), "Huston, we have some validation errors:\n" + String.join("\n", validationMessageList), "Validation Errors", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    conversionDTO.guiOrchestrator().setEnableSearchStepComponents(false);
                    startSearchForImagesStep();

                    ImageSearcherSwingWorker worker = new ImageSearcherSwingWorker(conversionDTO.guiOrchestrator().getSourceDirectory(), this::updateApplicationStatus, this::onSearchComplete);
                    worker.execute();

                });
    }

    private void onSearchComplete(List<Path> paths) {
        endSearchForImagesStep(paths);

    }

    public void startSearchForImagesStep() {
        conversionDTO.guiOrchestrator().resetConversionItemList();


        SwingUtilities.invokeLater(() -> {
            String message = "Searching for image...";
            applicationStatusLabel.setText(message);
            secondaryApplicationStatusLabel.setText("Searching for images...");
            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(false);
            conversionDTO.convertComponent().getMainActionButton().setEnabled(false);
        });
    }

    public void endSearchForImagesStep(List<Path> paths) {
        conversionDTO.guiOrchestrator().onSearchStepFinish(conversionDTO.convertComponent().getSelectedItem().getExtension(), paths);
        SwingUtilities.invokeLater(() -> {
            String message = String.format("Search step done! %s processable images found.", paths.size());
            applicationStatusLabel.setText(message);
            secondaryApplicationStatusLabel.setText("Search done: " + paths.size() + " image(s) found");
            applicationStatusLabel.setVisible(true);

            message = String.format("Processing %s images... Please wait.", paths.size());
            applicationStatusLabel.setText(message);
            secondaryApplicationStatusLabel.setText("Processing...");
        });
    }

    public void updateApplicationStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            applicationStatusLabel.setText(text);
        });
    }

    public void startConversion(List<ConversionItemDTO> list) {
        log.info(list);
        new ConversionWorker(list, this::updateList, this::onConversionDone).execute();
    }

    private void onConversionDone() {
        conversionDTO.guiOrchestrator().setEnableSearchStepComponents(true);
        conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(true);
        conversionDTO.convertComponent().getMainActionButton().setEnabled(true);

        DefaultListModel<ConversionItemDTO> model = (DefaultListModel<ConversionItemDTO>) conversionDTO.conversionFileList().getModel();

        List<ConversionItemDTO> list = new ArrayList<>();
        IntStream.range(0, model.getSize())
                .forEach(index -> {
                    list.add(model.get(index));
                });

        long failed = list.stream().filter(dto -> ConversionStatus.FAILED.equals(dto.getStatus())).count();
        long success = list.stream().filter(dto -> ConversionStatus.COMPLETED.equals(dto.getStatus())).count();

        String message = String.format("Conversion done! There were processed %s images, and we have %s successes / %s failures ", list.size(), success, failed);
        applicationStatusLabel.setText(message);
        secondaryApplicationStatusLabel.setText(String.format("Conversion done! %s successes / %s failures", success, failed));
    }

    private void updateList(List<ConversionItemDTO> list) {
        conversionDTO.guiOrchestrator().updateList(list);
    }


}
