package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionWorkerInputDTO;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.gui.worker.ConversionWorker;
import org.andreidodu.nocconvert.gui.worker.ImageSearcherSwingWorker;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.mapper.ConversionItemDTOMapper;
import org.andreidodu.nocconvert.helper.FileUtil;
import org.andreidodu.nocconvert.helper.ImageConverterUtil;
import org.andreidodu.nocconvert.helper.check.FormatCheckUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.exit;
import static org.andreidodu.nocconvert.helper.CpuUtil.calculateCpuLoadPercentage;
import static org.andreidodu.nocconvert.helper.OperationUtil.retryable;
import static org.andreidodu.nocconvert.helper.PathNameUtil.normalizePath;
import static org.andreidodu.nocconvert.helper.PathNameUtil.normalizePathAdvanced;

public class ConversionController {
    private static final Logger log = LogManager.getLogger(ConversionController.class);
    public static final int DELETE_SEMAPHORE_SIZE = 100;
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

    private final AtomicInteger passes = new AtomicInteger(0);
    private final AtomicInteger failures = new AtomicInteger(0);
    private final AtomicInteger totalFound = new AtomicInteger(0);

    private String timeElapsed = "0s";

    public ConversionController(ConversionDTO conversionDTO) {
        this.conversionDTO = conversionDTO;
        this.imageConverterUtil = new ImageConverterUtil();
        this.applicationStatusLabel = conversionDTO.applicationStatusLabel();
        this.secondaryApplicationStatusLabel = conversionDTO.secondaryApplicationStatusLabel();
        initializeConvertComponent();
    }

    public void initializeConvertComponent() {
        populateConvertComponentDropdownMenu();
        addConvertComponentEventListener();
    }

    private void populateConvertComponentDropdownMenu() {
        List<FormatExtensionDTO> imageFormatList = imageConverterUtil.getAvailableWriteFormatList();

        imageFormatList = FormatCheckUtil.testAvailableFormatsAndGet(imageFormatList);

        FormatExtensionDTO preferredFormat = imageFormatList.stream().filter(format -> "png".equalsIgnoreCase(format.getFormat())).findFirst().orElse(imageFormatList.getLast());

        conversionDTO.convertComponent().setImageList(imageFormatList);
        conversionDTO.convertComponent().setPreferredFormat(preferredFormat);
        conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.START);
    }


    public void addConvertComponentEventListener() {
        conversionDTO.convertComponent().getMainActionButton().addActionListener(e -> {

            if (SplitButtonComponent.Action.START.equals(conversionDTO.convertComponent().getAction())) {
                List<String> validationMessageList = conversionDTO.guiOrchestrator().getValidationMessageList();
                if (!validationMessageList.isEmpty()) {
                    JOptionPane.showMessageDialog(conversionDTO.guiOrchestrator(), "Huston, we have some validation errors:\n" + String.join("\n", validationMessageList), "Validation Errors", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                conversionDTO.guiOrchestrator().setEnableSearchStepComponents(false);
                startSearchForImagesStep();

                imageSearcherSwingWorker = new ImageSearcherSwingWorker(conversionDTO.guiOrchestrator().getSourceDirectory(), new FilesInDirectoryListenerImpl());
                imageSearcherSwingWorker.execute();
            } else if (SplitButtonComponent.Action.STOP.equals(conversionDTO.convertComponent().getAction())) {
                manualShutdownWithExit(false);
            }
        });
    }

    public ConversionWorker getWorker() {
        return this.conversionWorker;
    }

    public int getInternalSuccessCount() {
        return this.passes.get();
    }

    public int getInternalFailuresCount() {
        return this.failures.get();
    }

    public int getInternalTotalCount() {
        return this.totalFound.get();
    }


    private class FilesInDirectoryListenerImpl implements FilesInDirectoryListener {

        @Override
        public void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory) {
            SwingUtilities.invokeLater(() -> {
                if (filesInDirectory > 0) {
                    applicationStatusLabel.setText("<html><center style=\"font-weight: bold;color: #0078D4;\">Searching for images...</center>" + "<span style=\"color: #007bff;\">Found " + filesInDirectory + " file(s) in " + normalizePath(directory.getFileName().toString(), 30) + "</span></html>");
                } else {
                    applicationStatusLabel.setText("<html><center style=\"font-weight: bold;color: #0078D4;\">Searching for images...</center>" + "<span style=\"color: #007bff;\">" + normalizePathAdvanced(directory.toString(), DELETE_SEMAPHORE_SIZE) + "</spa></html>");
                }
            });
        }


        @Override
        public void onFileFound(Path filename) {
            SwingUtilities.invokeLater(() -> {
                applicationStatusLabel.setText("<html><center style=\"font-weight: bold;color: #0078D4;\">Searching for images...</center>" + "<span style=\"color: #007bff;\">" + normalizePathAdvanced(filename.getParent().toString(), 60) + " &#8658; " + normalizePath(filename.getFileName().toString(), 30) + "</span></html>");
            });
        }

        @Override
        public void onFileAnalyzationStart(int totalNumberFiles) {
            conversionDTO.guiOrchestrator().enableProgressBarPanelFromEDT();
            conversionDTO.guiOrchestrator().updateMainProgressBarMaxValue(totalNumberFiles);
            secondaryApplicationStatusLabel.setText("Validating search results...");
        }

        @Override
        public void onFileAnalyzationProgress() {
            conversionDTO.guiOrchestrator().incrementMainProgressBarProgress();
        }

        @Override
        public void onFileAnalyzation(Path filename) {
            SwingUtilities.invokeLater(() -> {
                applicationStatusLabel.setText("<html><center style=\"font-weight: bold;color: #0078D4;\">Format Identification and Image Header Validation...</center>" +
                        "<span style=\"color: #007bff;\">" + normalizePathAdvanced(filename.getParent().toString(), 60) + " &#8658; " + normalizePath(filename.getFileName().toString(), 30) + "</span></html>");
            });
        }

        @Override
        public void onUpdateTotalFile(Long numberOfFiles) {
            SwingUtilities.invokeLater(() -> {
                secondaryApplicationStatusLabel.setText("<html><span style=\"font-weight: bold;\">" + numberOfFiles + " Total Images</span></html>");
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

        @Override
        public void onAccessDenied() {
            conversionDTO.guiOrchestrator().onAccessDenied();
        }
    }

    private void onOperationAborted() {
        conversionDTO.guiOrchestrator().onOperationAborted();
    }


    public void manualShutdownWithExit(boolean exit) {
        Thread.ofVirtual().start(() -> manualShutdownWithExitAction(exit));
    }

    public void manualShutdownWithExitAction(boolean exit) {

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Void>> callableList = new ArrayList<>();

            if (conversionWorker != null && !conversionWorker.isDone()) {
                log.info("Cancelling active conversion process.");
                callableList.add(() -> {
                    return conversionWorker.shutdownThreads();
                });
            }

            if (imageSearcherSwingWorker != null && !imageSearcherSwingWorker.isDone()) {
                log.debug("Cancelling active search process.");
                callableList.add(() -> imageSearcherSwingWorker.shutdown());
            }

            Optional.ofNullable(conversionWorker).ifPresent(conversionWorker -> {
                ConcurrentLinkedQueue<Path> tmpDirectoryList = conversionWorker.getConversionOrchestrator().getFilesQueue();
                Path tmpDirectory = conversionWorker.getConversionOrchestrator().getTmpParentDirPath();
                callableList.add(() -> {
                    return deleteTemporaryFiles(tmpDirectoryList, tmpDirectory);
                });
            });

            callableList.add(() -> {
                if (imageSearcherSwingWorker == null) {
                    return null;
                }

                return imageSearcherSwingWorker.shutdown();
            });

            executorService.invokeAll(callableList);

            if (exit) {
                log.info("Exiting....");
                exit(0);
            }

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    private static Void deleteTemporaryFiles(ConcurrentLinkedQueue<Path> tmpDirPaths, Path tmpParentDirPath) {

        try (ExecutorService virtualThread = Executors.newVirtualThreadPerTaskExecutor()) {

            Semaphore vtSemaphore = new Semaphore(DELETE_SEMAPHORE_SIZE);

            Collection<Future<?>> tasks = new ArrayList<>();

            Future<?> tmpDirectoryCleanerFuture = virtualThread.submit(() -> {
                retryable(vtSemaphore, tmpParentDirPath, FileUtil::deleteDirectoryIfExists);
            });
            tasks.add(tmpDirectoryCleanerFuture);

            tmpDirPaths.forEach((path) -> {
                Future<?> tmpFileCleanerFuture = virtualThread.submit(() -> {
                    retryable(vtSemaphore, path.toFile(), FileUtil::deleteFileIfExists);
                });
                tasks.add(tmpFileCleanerFuture);
            });


            for (Future<?> task : tasks) {
                try {
                    task.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error while deleting temporary files and directory", e);
                }
            }

            return null;
        }
    }

    private void onSearchComplete(List<Path> paths) {
        endSearchForImagesStep(paths);
    }

    public void startSearchForImagesStep() {
        conversionDTO.guiOrchestrator().resetConversionItemList();
        totalFound.set(0);
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
        totalFound.set(paths.size());
        this.paths = paths;
        conversionDTO.guiOrchestrator().onSearchStepFinish(conversionDTO.convertComponent().getSelectedItem().getExtension(), paths);
    }

    public void startConversion(List<ConversionItemDTO> list) {
        ConversionItemDTOMapper conversionItemDTOMapper = new ConversionItemDTOMapper();
        list = list.stream().map(conversionItemDTOMapper::clone).toList();
        conversionDTO.guiOrchestrator().onConversionStart();

        this.processedItems = 0;
        this.past = new Date();

        failures.set(0);
        passes.set(0);


        ConversionWorkerInputDTO conversionWorkerInputDTO = buildInput(list);
        conversionWorker = new ConversionWorker(conversionWorkerInputDTO);
        conversionWorker.execute();
        conversionDTO.guiOrchestrator().updateMainProgressBarMaxValue(list.size());
        conversionDTO.guiOrchestrator().resetProgressBarPanelFromEDT();
        conversionDTO.guiOrchestrator().enableProgressBarPanelFromEDT();

    }

    private ConversionWorkerInputDTO buildInput(List<ConversionItemDTO> list) {
        return ConversionWorkerInputDTO.builder()
                .list(list)
                .updateGui(this::updateList)
                .onAllTasksComplete(this::onAllItemsCompleted)
                .completeItem(this::onItemCompleted)
                .incrementPasses(this::incrementPasses)
                .incrementFailures(this::incrementFailures)
                .build();
    }

    private void incrementFailures() {
        this.failures.incrementAndGet();
    }

    private void incrementPasses() {
        this.passes.incrementAndGet();
    }

    public boolean isWorking() {
        return SplitButtonComponent.Action.STOP.equals(conversionDTO.convertComponent().getAction());
    }

    private void onItemCompleted(ConversionItemDTO item) {
        conversionDTO.guiOrchestrator().incrementMainProgressBarProgress();
        processedItems++;
        if (!isWorking()) {
            return;
        }

        Optional.ofNullable(paths).ifPresent(paths -> {
            applicationStatusLabel.setText("Converted " + processedItems + " of " + paths.size() + " Images (" + failures.get() + " failures). Please wait.");
            long now = System.currentTimeMillis();
            if (now - lastTime >= 1000) {
                lastTime = now;
                this.cpuLoadPercentage = calculateCpuLoadPercentage();
                this.timeElapsed = calculateTimeElapsed();
            }
            if (ApplicationConfig.DEV_MODE) {
                conversionDTO.secondaryApplicationStatusLabel().setText(timeElapsed + " | " + cpuLoadPercentage + "% CPU load | " + (conversionWorker != null ? conversionWorker.getPlatformThreadsPermits() : -1) + " P-Threads | " + (conversionWorker != null ? conversionWorker.getVirtualThreadsPermits() : -1) + " V-Threads | Processed " + processedItems + "/" + paths.size() + " Images");
                return;
            }

            conversionDTO.secondaryApplicationStatusLabel().setText(timeElapsed + " | Processed " + processedItems + " / " + paths.size() + " Images");
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

    public void onAllItemsCompleted() {
        conversionDTO.guiOrchestrator().onConversionDone();
        SwingUtilities.invokeLater(() -> {
            conversionDTO.guiOrchestrator().setEnableSearchStepComponents(true);
            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(true);
        });

        conversionDTO.guiOrchestrator().hideProgressBar();
        conversionDTO.guiOrchestrator().onConversionDone();

        int total = totalFound.get();
        int passed = passes.get();
        int failed = failures.get();
        int unprocessed = totalFound.get() - failed - passed;

        String colorPassed = passed > 0 ? "green" : "white";
        String colorUnprocessed = totalFound.get() - passed - failed > 0 ? "#CCCC00" : "white";
        String colorFailed = failed > 0 ? "red" : "white";

        String coloredMessage = String.format("<html><div style='text-align:center;'>" + "<span style='font-size:14pt;'>Conversion completed.</span><br/>" + "<span style='font-size:14pt; font-weight:bold;'>Total: </span>" + "<span style='font-size:14pt; font-weight:bold;'>%s. </span>" + "<span style='color:" + colorPassed + "; font-weight:bold; font-size:14pt;'>Success: %s / </span>" + "<span style='color:" + colorUnprocessed + "; font-weight:bold; font-size:14pt;'>Unprocessed: %s / </span>" + "<span style='color:" + colorFailed + "; font-weight:bold; font-size:14pt;'>Failed: %s</span>" + "</div></html>", total, passed, unprocessed, failed);

        SwingUtilities.invokeLater(() -> {
            applicationStatusLabel.setText(coloredMessage);
            secondaryApplicationStatusLabel.setText(String.format("Conversion done! %s successes / %s failures", passed, failed));
            conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.START);
        });
    }

    private void updateList(List<ConversionItemDTO> list) {
        conversionDTO.guiOrchestrator().updateList(list);
    }

    public void resetConversionButton() {
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
