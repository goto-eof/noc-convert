package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.gui.worker.ConversionWorker;
import org.andreidodu.nocconvert.gui.worker.ImageSearcherSwingWorker;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.mapper.ConversionItemDTOMapper;
import org.andreidodu.nocconvert.util.ImageConverterUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.andreidodu.nocconvert.util.CpuUtil.calculateCpuLoadPercentage;

public class ConversionController {
    private static final Logger log = LogManager.getLogger(ConversionController.class);
    private final ConversionDTO conversionDTO;
    private final ImageConverterUtil imageConverterUtil;
    private final JLabel applicationStatusLabel;
    private final JLabel secondaryApplicationStatusLabel;
    private ConversionWorker conversionWorker;
    private ImageSearcherSwingWorker imageSearcherSwingWorker;
    private List<Path> paths;
    private int processedItems;
    private int cpuLoadPercentage;
    private long lastTime = System.currentTimeMillis();
    private Date past = new Date();

    private String timeElapsed = "0s";

    public ConversionController(ConversionDTO conversionDTO) {
        this.conversionDTO = conversionDTO;
        this.imageConverterUtil = new ImageConverterUtil();
        this.applicationStatusLabel = conversionDTO.applicationStatusLabel();
        this.secondaryApplicationStatusLabel = conversionDTO.secondaryApplicationStatusLabel();
        initializeConvertComponent();
    }

    private void initializeConvertComponent() {
        populateConvertComponentDropdownMenu();
        addConvertComponentEventListener();
        initializeDestinationDirectoryButton();
    }

    private void initializeDestinationDirectoryButton() {
        SwingUtilities.invokeLater(() -> {
            conversionDTO.openDestinationDirectoryButton().setVisible(false);
        });

        conversionDTO.openDestinationDirectoryButton().getButton().addActionListener(e -> {
            Path destinationDirectory = conversionDTO.guiOrchestrator().getDestinationDirectory();
            if (destinationDirectory == null || !destinationDirectory.toFile().exists()) {
                JOptionPane.showMessageDialog(null, "Destination directory does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File directory = destinationDirectory.toFile();

            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(directory);
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

    private void populateConvertComponentDropdownMenu() {
        List<FormatExtensionDTO> imageFormatList = imageConverterUtil.getAvailableWriteFormatList();
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

                        imageSearcherSwingWorker = new ImageSearcherSwingWorker(conversionDTO.guiOrchestrator().getSourceDirectory(), this::updateApplicationStatus, new FilesInDirectoryListenerImpl());
                        imageSearcherSwingWorker.execute();
                    } else if (SplitButtonComponent.Action.STOP.equals(conversionDTO.convertComponent().getAction())) {
                        cancelActiveProcess();
                    }
                });
    }


    private class FilesInDirectoryListenerImpl implements FilesInDirectoryListener {

        @Override
        public void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory) {
            SwingUtilities.invokeLater(() -> {
                applicationStatusLabel.setText("found " + filesInDirectory + " file(s) in " + normalizeString(directory.getFileName().toString()));
            });
        }

        private String normalizeString(String string) {
            if (string == null || string.isEmpty()) {
                return "";
            }

            if (string.length() > 30) {
                return string.substring(0, 30);
            }

            return string;
        }

        @Override
        public void onFileFound(Path filename) {
            SwingUtilities.invokeLater(() -> {
                applicationStatusLabel.setText("File: " + filename.getFileName().toString());
            });
        }

        @Override
        public void onUpdateTotalFile(Long numberOfFiles) {
            SwingUtilities.invokeLater(() -> {
                secondaryApplicationStatusLabel.setText(numberOfFiles + " Total Images");
            });
        }

        @Override
        public void onSearchDone(List<Path> result) {
            onSearchComplete(result);
        }

        @Override
        public void onOperationAborted() {
            ConversionController.this.onOperationAborted();
        }
    }

    private void onOperationAborted() {
        conversionDTO.guiOrchestrator().onOperationAborted();
    }


    private void cancelActiveProcess() {

        if (conversionWorker != null && !conversionWorker.isDone()) {
            log.info("Cancelling active conversion process.");
            conversionWorker.shutdown(true);
        }

        if (imageSearcherSwingWorker != null && !imageSearcherSwingWorker.isDone()) {
            imageSearcherSwingWorker.shutdown(true);
        }

        onConversionDone();
    }

    private void onSearchComplete(List<Path> paths) {
        endSearchForImagesStep(paths);
        imageSearcherSwingWorker = null;
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
        this.paths = paths;
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

        SwingUtilities.invokeLater(() -> {
            conversionDTO.openDestinationDirectoryButton().setVisible(true);
        });

        this.processedItems = 0;
        this.past = new Date();

        conversionWorker = new ConversionWorker(list, this::updateList, this::onConversionDone, this::onItemCompleted, this::onAllTasksComplete);
        conversionWorker.execute();
        conversionDTO.guiOrchestrator().updateMainProgressBarMaxValue(list.size());
    }

    public boolean isWorking() {
        return SplitButtonComponent.Action.STOP.equals(conversionDTO.convertComponent().getAction());
    }

    private void onAllTasksComplete() {
        conversionDTO.guiOrchestrator().onConversionDone();
    }

    private void onItemCompleted(ConversionItemDTO item) {
        conversionDTO.guiOrchestrator().incrementMainProgressBarProgress();
        processedItems++;
        if (!isWorking()) {
            return;
        }

        Optional.ofNullable(paths).ifPresent(paths -> {
            applicationStatusLabel.setText("Converted " + processedItems + " of " + paths.size() + " Images. Please wait.");
            long now = System.currentTimeMillis();
            if (now - lastTime >= 1000) {
                lastTime = now;
                this.cpuLoadPercentage = calculateCpuLoadPercentage();
                this.timeElapsed = calculateTimeElapsed();
            }
            conversionDTO.secondaryApplicationStatusLabel().setText(timeElapsed + " | " + cpuLoadPercentage + "% CPU load | " + conversionWorker.getPlatformThreadsPermits() + " P-Threads | " + conversionWorker.getVirtualThreadsPermits() + " V-Threads | Processed " + processedItems + "/" + paths.size() + " Images");
        });
    }

    private String calculateTimeElapsed() {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        Date now = new Date();

        long seconds = TimeUnit.MILLISECONDS.toSeconds(now.getTime() - past.getTime());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime());
        long hours = TimeUnit.MILLISECONDS.toMinutes(now.getTime() - past.getTime());
        long days = TimeUnit.MILLISECONDS.toDays(now.getTime() - lastTime);

        if (seconds < 60) {
            return seconds + "s";
        }

        if (minutes < 60) {
            return minutes + "m";
        }

        if (hours < 24) {
            return hours + "h";
        }

        return days + "d";
    }

    private void onConversionDone() {
        conversionWorker = null;
        SwingUtilities.invokeLater(() -> {
            conversionDTO.guiOrchestrator().setEnableSearchStepComponents(true);
            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(true);
        });

        conversionDTO.guiOrchestrator().hideProgressBar();
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

        long other = list.stream()
                .filter(item -> !List.of(ConversionStatus.FAILED, ConversionStatus.COMPLETED, ConversionStatus.QUEUED, ConversionStatus.PROCESSING).contains(item.getStatus()))
                .count();
        log.debug("unknown status: {}", other);


        SwingUtilities.invokeLater(() -> {

            String colorSuccess = success > 0 ? "green" : "white";
            String colorUnprocessed = queued + processing > 0 ? "#CCCC00" : "white";
            String colorFailed = failed > 0 ? "red" : "white";

            String coloredMessage = String.format("<html><div style='text-align:center;'>" +
                    "<span style='font-size:14pt;'>Conversion completed.</span><br/>" +
                    "<span style='font-size:14pt; font-weight:bold;'>Total: </span>" + "<span style='font-size:14pt; font-weight:bold;'>%s. </span>" +
                    "<span style='color:" + colorSuccess + "; font-weight:bold; font-size:14pt;'>Success: %s / </span>" +
                    "<span style='color:" + colorUnprocessed + "; font-weight:bold; font-size:14pt;'>Unprocessed: %s / </span>" +
                    "<span style='color:" + colorFailed + "; font-weight:bold; font-size:14pt;'>Failed: %s</span>" +
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
        SwingUtilities.invokeLater(() -> {
            enableConvertButtons(true);
            conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.START);
        });
    }

    public void enableConvertButtons(boolean value) {
        SwingUtilities.invokeLater(() -> {
            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(value);
            conversionDTO.convertComponent().getMainActionButton().setEnabled(value);
        });

    }
}
