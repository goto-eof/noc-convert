package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.gui.worker.ConversionWorker;
import org.andreidodu.nocconvert.gui.worker.ImageSearcherSwingWorker;
import org.andreidodu.nocconvert.mapper.ConversionItemDTOMapper;
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
        conversionDTO.convertComponent().setImageList(imageFormatList);
        conversionDTO.convertComponent().setPreferredFormat(preferredFormat);
        conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.START);
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
        if (paths.isEmpty()) {
            conversionDTO.guiOrchestrator().noFilesFound();
            return;
        }
        conversionDTO.guiOrchestrator().onSearchStepFinish(conversionDTO.convertComponent().getSelectedItem().getExtension(), paths);
    }

    public void updateApplicationStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            applicationStatusLabel.setText(text);
        });
    }

    public void startConversion(List<ConversionItemDTO> list) {
        ConversionItemDTOMapper conversionItemDTOMapper = new ConversionItemDTOMapper();
        list = list.stream().map(conversionItemDTOMapper::clone).toList();
        conversionDTO.guiOrchestrator().onConversionStart();
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

        conversionDTO.guiOrchestrator().onConversionDone();

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
            String coloredMessage = String.format("<html><div style='text-align:center;'>" +
                    "<span style='font-size:14pt;'>Conversion completed.</span><br/>" +
                    "<span style='font-size:14pt; font-weight:bold;'>Total: </span>" + "<span style='font-size:14pt; font-weight:bold;'>%s. </span>" +
                    "<span style='color:green; font-weight:bold; font-size:14pt;'>Success: %s / </span>" +
                    "<span style='color:#CCCC00; font-weight:bold; font-size:14pt;'>Unprocessed: %s / </span>" +
                    "<span style='color:red; font-weight:bold; font-size:14pt;'>Failed: %s</span>" +
                    "</div></html>", list.size(), success, queued + processing, failed);


            applicationStatusLabel.setText(coloredMessage);
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

    public void noFileFound() {
        conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(true);
        conversionDTO.convertComponent().getMainActionButton().setEnabled(true);
        conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.START);

    }

    public void enableButtons(boolean b) {
        conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(b);
        conversionDTO.convertComponent().getMainActionButton().setEnabled(b);
    }
}
