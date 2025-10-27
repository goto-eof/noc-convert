package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.gui.worker.ConversionWorker;
import org.andreidodu.nocconvert.gui.worker.ImageSearcherSwingWorker;
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
    private ConversionWorker conversionWorker;
    private ImageSearcherSwingWorker imageSearcherSwingWorker;

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
        conversionDTO.convertComponent().init(imageFormatList, preferredFormat, SplitButtonComponent.Action.START);
    }

    private void addConvertComponentEventListener() {
        conversionDTO.convertComponent().getMainActionButton()
                .addActionListener(e -> {

                    if (SplitButtonComponent.Action.START.equals(conversionDTO.convertComponent().getAction())) {
                        List<String> validationMessageList = conversionDTO.guiOrchestrator().getValidationMessageList();
                        if (!validationMessageList.isEmpty()) {
                            JOptionPane.showMessageDialog(conversionDTO.guiOrchestrator(), "Huston, we have some validation errors:\n" + String.join("\n", validationMessageList), "Validation Errors", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        conversionDTO.guiOrchestrator().setEnableSearchStepComponents(false);
                        startSearchForImagesStep();

                        imageSearcherSwingWorker = new ImageSearcherSwingWorker(conversionDTO.guiOrchestrator().getSourceDirectory(), this::updateApplicationStatus, this::onSearchComplete);
                        imageSearcherSwingWorker.execute();
                    } else if (SplitButtonComponent.Action.STOP.equals(conversionDTO.convertComponent().getAction())) {
                        if (conversionWorker != null) {
                            cancelActiveProcess();
                        }
                    }
                });
    }


    private void cancelActiveProcess() {
        if (conversionWorker != null && !conversionWorker.isDone()) {
            log.info("Cancelling active conversion process.");
            conversionWorker.shutdown(true);
        } else if (imageSearcherSwingWorker != null && !imageSearcherSwingWorker.isDone()) {
            imageSearcherSwingWorker.cancel(true);
            onConversionDone();
        }
    }

    private void onSearchComplete(List<Path> paths) {
        endSearchForImagesStep(paths);
    }

    public void startSearchForImagesStep() {
        conversionDTO.guiOrchestrator().resetConversionItemList();

        SwingUtilities.invokeLater(() -> {
            conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.STOP);
            String message = "Searching for image...";
            applicationStatusLabel.setText(message);
            secondaryApplicationStatusLabel.setText("Searching for images...");
            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(false);
        });

        conversionDTO.guiOrchestrator().startSearch();
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
        conversionWorker = new ConversionWorker(list, this::updateList, this::onConversionDone, this::onItemCompleted);
        conversionWorker.execute();
        conversionDTO.guiOrchestrator().updateMainProgressBarMaxValue(list.size());

    }

    private void onItemCompleted() {
        conversionDTO.guiOrchestrator().incrementMainProgressBarProgress();
    }

    private void onConversionDone() {
        conversionDTO.guiOrchestrator().setEnableSearchStepComponents(true);
        conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(true);

        conversionDTO.guiOrchestrator().conversionDone();

        DefaultListModel<ConversionItemDTO> model = (DefaultListModel<ConversionItemDTO>) conversionDTO.conversionFileList().getModel();

        List<ConversionItemDTO> list = new ArrayList<>();
        IntStream.range(0, model.getSize())
                .forEach(index -> {
                    list.add(model.get(index));
                });

        long failed = list.stream().filter(dto -> ConversionStatus.FAILED.equals(dto.getStatus())).count();
        long success = list.stream().filter(dto -> ConversionStatus.COMPLETED.equals(dto.getStatus())).count();
        long queued = list.stream().filter(dto -> ConversionStatus.QUEUED.equals(dto.getStatus())).count();
        long processing = list.stream().filter(dto -> ConversionStatus.PROCESSING.equals(dto.getStatus())).count();

        SwingUtilities.invokeLater(() -> {
            String message = String.format("Conversion completed. Total: %s. Successes: %s / Unprocessed: %s / Failures: %s", list.size(), success, queued + processing, failed);
            applicationStatusLabel.setText(message);
            secondaryApplicationStatusLabel.setText(String.format("Conversion done! %s successes / %s failures", success, failed));
            conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.START);
        });

    }

    private void updateList(List<ConversionItemDTO> list) {
        conversionDTO.guiOrchestrator().updateList(list);
    }


    public void shutdown() {
        cancelActiveProcess();
    }
}
