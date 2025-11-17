package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.conversion.input.SearchAndConvertInputDTO;
import org.andreidodu.nocconvert.exception.ManualAbortedException;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.gui.worker.hybrid.SearchAndConvertWorker;
import org.andreidodu.nocconvert.gui.worker.hybrid.util.PictureCounterTask;
import org.andreidodu.nocconvert.helper.ImageConverterUtil;
import org.andreidodu.nocconvert.helper.check.FormatCheckUtil;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.task.AlwaysCleanTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.exit;
import static org.andreidodu.nocconvert.helper.CpuUtil.calculateCpuLoadPercentage;
import static org.andreidodu.nocconvert.helper.NumberUtil.buildNumberFormatter;
import static org.andreidodu.nocconvert.helper.PathNameUtil.normalizePath;
import static org.andreidodu.nocconvert.helper.PathNameUtil.normalizePathAdvanced;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ConversionController {
    private static final Logger log = LogManager.getLogger(ConversionController.class);
    public static final int DELETE_SEMAPHORE_SIZE = 100;
    private static final int UI_UPDATE_INTERVAL = 200;
    private final AtomicLong counter = new AtomicLong(0);
    private final ConversionDTO conversionDTO;
    private final ImageConverterUtil imageConverterUtil;
    private final JLabel applicationStatusLabel;
    private final JLabel secondaryApplicationStatusLabel;
    private SearchAndConvertWorker searchAndConvertWorker;
    private final List<Path> paths = new ArrayList<>();
    private final AtomicInteger processedItems = new AtomicInteger(0);
    private final AtomicInteger cpuLoadPercentage = new AtomicInteger(0);
    private long lastTime = System.currentTimeMillis();
    private Date past = new Date();

    private final AtomicInteger passes = new AtomicInteger(0);
    private final AtomicInteger failures = new AtomicInteger(0);
    private final AtomicInteger totalFound = new AtomicInteger(0);

    private final AtomicReference<String> timeElapsed = new AtomicReference<>("0s");
    private static final BlockingQueue<Path> globalDeletionQueue = new LinkedBlockingQueue<>();

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
                startCountForImagesStep();
//                FilesInDirectoryListenerImpl filesInDirectoryListener = new FilesInDirectoryListenerImpl(this);
//                imageSearcherSwingWorker = new ImageSearcherSwingWorker(conversionDTO.guiOrchestrator().getSourceDirectory(), filesInDirectoryListener, this::endSearchForImagesStep);
//                imageSearcherSwingWorker.execute();

                startConversion();
            } else if (SplitButtonComponent.Action.STOP.equals(conversionDTO.convertComponent().getAction())) {
                manualShutdownWithExit(false);
            }
        });
    }

    public SearchAndConvertWorker getWorker() {
        return this.searchAndConvertWorker;
    }

    public int getInternalSuccessCount() {
        return this.passes.get();
    }


    private class FilesInDirectoryListenerImpl implements FilesInDirectoryListener {
        private final ConversionController parent;

        public FilesInDirectoryListenerImpl(ConversionController conversionController) {
            parent = conversionController;
        }

        @Override
        public void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory) {
            SwingUtilities.invokeLater(() -> {
                if (filesInDirectory > 0) {
                    applicationStatusLabel.setText("<html><center style=\"font-weight: bold;color: #0078D4;\">Searching for images (" + totalFiles + ")...</center>" +
                            "<span style=\"color: #007bff;\">Found " + filesInDirectory + " file(s) in " + normalizePath(directory.getFileName().toString(), 30) + "</span></html>");
                } else {
                    applicationStatusLabel.setText("<html><center style=\"font-weight: bold;color: #0078D4;\">Counting images...</center>" +
                            "<span style=\"color: #007bff;\">" + normalizePathAdvanced(directory.toString(), DELETE_SEMAPHORE_SIZE) + "</spa></html>");
                }
            });
        }


        @Override
        public void onFileFound(long totalFiles, Path filename) {
            onFileCount(totalFiles, filename);
        }

        @Override
        public void onFileAnalyzationStart(int totalNumberFiles) {
//            conversionDTO.guiOrchestrator().enableProgressBarPanelFromEDT();
//            conversionDTO.guiOrchestrator().updateMainProgressBarMaxValue(totalNumberFiles);
//            secondaryApplicationStatusLabel.setText("Validating search results...");
        }

        @Override
        public void onFileAnalyzationProgress(Integer totalNumberOfUpdates) {
//            conversionDTO.guiOrchestrator().incrementMainProgressBarProgress(totalNumberOfUpdates);
        }

        @Override
        public void onFileAnalyzationComplete(Path filename) {
//            long now = System.currentTimeMillis();
//            if (now - lastTime >= 1000) {
//
//                parent.cpuLoadPercentage.set(calculateCpuLoadPercentage());
//                parent.timeElapsed.set(calculateTimeElapsed());
//                String normalizedPath = normalizePathAdvanced(filename.getParent().toString(), 60);
//                String normalizedFilename = normalizePath(filename.getFileName().toString(), 30);
//                if (ApplicationConfig.DEV_MODE) {
//                    String mainMessage = "<html>" +
//                            "<center style=\"font-weight: bold;color: #0078D4;\">Format Identification and Image Header Validation...</center>" +
//                            "<span style=\"color: #007bff;\">" + normalizedPath + " &#8658; " + normalizedFilename + "</span>" +
//                            "</html>";
//                    String secondaryMessage = timeElapsed + " | " +
//                            cpuLoadPercentage + "% CPU load | Phase 1.5 - Validation search result...";
//
//                    SwingUtilities.invokeLater(() -> {
//                        applicationStatusLabel.setText(mainMessage);
//                        conversionDTO.secondaryApplicationStatusLabel().setText(secondaryMessage);
//                    });
//                    lastTime = now;
//                    return;
//                }
//                String mainMessage = "<html>" +
//                        "<center style=\"font-weight: bold;color: #0078D4;\">Format Identification and Image Header Validation...</center>" +
//                        "<span style=\"color: #007bff;\">" + normalizedPath + " &#8658; " + normalizedFilename + "</span>" +
//                        "</html>";
//                String secondaryMessage = "Phase 1.5 - Validation search result...";
//
//                SwingUtilities.invokeLater(() -> {
//                    applicationStatusLabel.setText(mainMessage);
//                    conversionDTO.secondaryApplicationStatusLabel().setText(secondaryMessage);
//                });
//                lastTime = now;
//            }
        }

        @Override
        public void onUpdateTotalFile(Long numberOfFiles) {
            DecimalFormat df = buildNumberFormatter();
            String numberOfFilesFormatted = df.format(numberOfFiles);
            SwingUtilities.invokeLater(() -> {
                secondaryApplicationStatusLabel.setText("<html><span style=\"font-weight: bold;\">" + numberOfFilesFormatted + " Total Images</span></html>");
            });
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

    private void onFileCount(long totalFiles, Path filename) {
        SwingUtilities.invokeLater(() -> {
            applicationStatusLabel.setText("<html><center style=\"font-weight: bold;color: #0078D4;\">Counting images (" + totalFiles + ")...</center>" +
                    "<span style=\"color: #007bff;\">" + normalizePathAdvanced(filename.getParent().toString(), 60) + " &#8658; " + normalizePath(filename.getFileName().toString(), 30) + "</span></html>");
        });
    }

    private void onFileFound(long totalFiles, Path filename) {
        SwingUtilities.invokeLater(() -> {
            applicationStatusLabel.setText("<html><center style=\"font-weight: bold;color: #0078D4;\">Searching for images (" + totalFiles + ")...</center>" +
                    "<span style=\"color: #007bff;\">" + normalizePathAdvanced(filename.getParent().toString(), 60) + " &#8658; " + normalizePath(filename.getFileName().toString(), 30) + "</span></html>");
        });
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

            if (searchAndConvertWorker != null && !searchAndConvertWorker.isDone()) {
                log.info("Cancelling active conversion process.");
                callableList.add(() -> searchAndConvertWorker.manualShutdownThreads());
            }

//            if (imageSearcherSwingWorker != null && !imageSearcherSwingWorker.isDone()) {
//                log.debug("Cancelling active search process.");
//                callableList.add(() -> imageSearcherSwingWorker.shutdown());
//            }
//
//
//            Optional.ofNullable(imageSearcherSwingWorker).ifPresent(ImageSearcherSwingWorker::shutdown);

            executorService.invokeAll(callableList);

            if (exit) {
                exitFromJVM();
            }

        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

    }

    private static void exitFromJVM() {
        log.info("Exit Procedure initialized....");
        log.info("OS instructions to delete temporary files");
        Thread.ofVirtual().start(new AlwaysCleanTask(globalDeletionQueue));
        log.info("bye bye from noc-convert :)");
        exit(0);
    }

    public void startCountForImagesStep() {
        //conversionDTO.guiOrchestrator().resetConversionItemList();
        totalFound.set(0);
        SwingUtilities.invokeLater(() -> {
            conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.STOP);
            String message = "Counting images...";
            applicationStatusLabel.setText(message);
            secondaryApplicationStatusLabel.setText("Phase 1 - Counting images...");
            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(false);
        });

        conversionDTO.guiOrchestrator().startSearch();
    }

//    public void startSearchForImagesStep() {
//        conversionDTO.guiOrchestrator().resetConversionItemList();
//        totalFound.set(0);
//        SwingUtilities.invokeLater(() -> {
//            conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.STOP);
//            String message = "Searching for image...";
//            applicationStatusLabel.setText(message);
//            secondaryApplicationStatusLabel.setText("Phase 1 - Searching for images...");
//            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(false);
//        });
//
//        conversionDTO.guiOrchestrator().startSearch();
//    }

    public void endSearchChunkForImagesStep(List<ConversionItemDTO> conversionItemDTOList) {
        Objects.requireNonNull(conversionItemDTOList, "conversionItemDTOList cannot be null bro");
        if (conversionItemDTOList == null || conversionItemDTOList.isEmpty()) {
            conversionDTO.guiOrchestrator().noFilesFound();
            return;
        }
        // conversionDTO.guiOrchestrator().enableJListAndProgressBar(true);
        conversionDTO.guiOrchestrator().addPathsToTheJList(conversionItemDTOList);
        totalFound.set(totalFound.get() + conversionItemDTOList.size());
        this.paths.clear();
        // conversionDTO.guiOrchestrator().onSearchStepFinish(conversionDTO.convertComponent().getSelectedItem().getExtension(), conversionItemDTOList);
    }

    public void startConversion() {
        // ConversionItemDTOMapper conversionItemDTOMapper = new ConversionItemDTOMapper();
        // list = list.stream().map(conversionItemDTOMapper::clone).toList();
        conversionDTO.guiOrchestrator().onConversionStart();

        this.processedItems.set(0);
        this.past = new Date();

        failures.set(0);
        passes.set(0);

        ExecutorService newVirtualThreadPerTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture.runAsync(() -> {
            try {
                FilesInDirectoryListenerImpl filesInDirectoryListener = new FilesInDirectoryListenerImpl(this);
                int totalNumberOfFiles = Math.toIntExact(new PictureCounterTask()
                        .count(conversionDTO.guiOrchestrator().getSourceDirectory(), filesInDirectoryListener));
                conversionDTO.guiOrchestrator().updateMainProgressBarMaxValue(totalNumberOfFiles);
            } catch (Exception e) {
                if (e instanceof ManualAbortedException) {
                    onOperationAborted();
                    return;
                }
                log.error(getRootCauseMessage(e), e);
                conversionDTO.guiOrchestrator().updateMainProgressBarMaxValue(0);
            }
        }, newVirtualThreadPerTaskExecutor).thenRunAsync(() -> {
            conversionDTO.guiOrchestrator().resetProgressBarPanelFromEDT();
            conversionDTO.guiOrchestrator().enableProgressBarPanelFromEDT();
            conversionDTO.guiOrchestrator().resetJList();

            SearchAndConvertInputDTO searchAndConvertInputDTO = buildInput();
            searchAndConvertWorker = new SearchAndConvertWorker(searchAndConvertInputDTO);
            searchAndConvertWorker.execute();
        }, newVirtualThreadPerTaskExecutor);

//        ConversionWorkerInputDTO conversionWorkerInputDTO = buildInput(list);
//        conversionWorker = new ConversionWorker(conversionWorkerInputDTO);
//        conversionWorker.execute();
        //conversionDTO.guiOrchestrator().updateMainProgressBarMaxValue(list.size());


    }

    private SearchAndConvertInputDTO buildInput() {
        return SearchAndConvertInputDTO.builder()
                .updateGui(this::updateList)
                .onAllTasksComplete(this::onAllItemsCompleted)
                .completeItem(this::onItemCompleted)
                .onConversionAborted(this::onConvertionAborted)
                .incrementPasses(this::incrementPasses)
                .incrementFailures(this::incrementFailures)
                .decrementPasses(this::decrementPasses)
                .addToDeletionQueue(this::addToDeletionQueue)
                .sourceDirectory(conversionDTO.guiOrchestrator().getSourceDirectory())
                .destinationDirectory(conversionDTO.guiOrchestrator().getDestinationDirectory())
                .targetFormat(conversionDTO.convertComponent().getSelectedItem().getFormat())
                .endSearchChunkForImagesStep(this::endSearchChunkForImagesStep)
                .incrementMainProgressBarProgress(conversionDTO.guiOrchestrator()::incrementMainProgressBarProgress)
                .onBucketItemsCompleted(this::onBucketItemsCompleted)
                .resetSecondaryProgressBar(this::resetSecondaryProgressBar)
                .updateSecondaryProgressBarMaxValue(conversionDTO.guiOrchestrator()::updateSecondaryProgressBarMaxValue)
                .updateSecondaryProgressBarColorColorToBlue(conversionDTO.guiOrchestrator()::updateSecondaryProgressBarColorColorToBlue)
                .onFileFound(this::onFileFound)
                .showJListPane(conversionDTO.guiOrchestrator()::showJListPane)
                .build();
    }

    private void resetSecondaryProgressBar() {
        conversionDTO.guiOrchestrator().resetSecondaryProgressBar();
    }


    private void incrementFailures() {
        this.failures.incrementAndGet();
    }

    private void incrementPasses() {
        this.passes.incrementAndGet();
    }

    private void decrementPasses() {
        this.passes.decrementAndGet();
    }

    public boolean isWorking() {
        return SplitButtonComponent.Action.STOP.equals(conversionDTO.convertComponent().getAction());
    }

    private void onItemCompleted(ConversionItemDTO item) {
        processedItems.incrementAndGet();
        if (!isWorking()) {
            return;
        }

        int cpuPercentage = calculateCpuLoadPercentage();

        SwingUtilities.invokeLater(() -> {


            DecimalFormat df = buildNumberFormatter();

            String processedItemsFormatted = df.format(processedItems.get());
            String totalFormatted = df.format(paths == null ? 0 : paths.size());
            String failureformatted = df.format(failures.get());

            Optional.ofNullable(paths).ifPresent(paths -> {
                if (paths.size() > 10_000 && counter.get() % UI_UPDATE_INTERVAL != 0) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - lastTime >= 1000) {

                    int startIndex = conversionDTO.conversionFileJList().getFirstVisibleIndex();
                    int endIndex = conversionDTO.conversionFileJList().getLastVisibleIndex();

                    if (item.getIndex() >= startIndex && item.getIndex() <= endIndex) {
                        conversionDTO.conversionFileJList().repaint();
                    }

                    applicationStatusLabel.setText("Converted " + processedItemsFormatted + " of " + totalFormatted + " Images (" + failureformatted + " failures). Please wait.");
                    conversionDTO.guiOrchestrator().incrementSecondaryProgressBarProgress(1);
                    lastTime = now;
                    this.cpuLoadPercentage.set(cpuPercentage);
                    this.timeElapsed.set(calculateTimeElapsed());
                    if (ApplicationConfig.DEV_MODE) {
                        conversionDTO.secondaryApplicationStatusLabel().setText(timeElapsed + " | " +
                                cpuLoadPercentage + "% CPU load | " +
                                (searchAndConvertWorker != null ? searchAndConvertWorker.getPlatformThreadsPermits() : "default") +
                                " P-Threads | " + (searchAndConvertWorker != null ? searchAndConvertWorker.getVirtualThreadsPermits() : "default") +
                                " V-Threads | Phase 3 - Processed " + processedItemsFormatted + "/" + totalFormatted + " Images");
                        return;
                    }
                    conversionDTO.secondaryApplicationStatusLabel().setText(timeElapsed + " | Phase 3 - Processed " + processedItemsFormatted + " / " + totalFormatted + " Images");
                }

            });
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

    public void onBucketItemsCompleted() {
        //conversionDTO.guiOrchestrator().onConversionDone();
//        SwingUtilities.invokeLater(() -> {
//            conversionDTO.guiOrchestrator().setEnableSearchStepComponents(true);
//            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(true);
//        });

//        conversionDTO.guiOrchestrator().hideProgressBar();
//        conversionDTO.guiOrchestrator().onConversionDone();

//        String coloredMessage = buildColoredFinishMessage("<span style='font-size:18pt; color:green; fond-weight:bold;'>Conversion completed</span>");

//        DecimalFormat df = buildNumberFormatter();
//        String formattedNumberPassed = df.format(passes.get());
//        String formattedNumberFailed = df.format(failures.get());
//
//        SwingUtilities.invokeLater(() -> {
//            applicationStatusLabel.setText(coloredMessage);
//            secondaryApplicationStatusLabel.setText(String.format("Conversion done! %s successes / %s failures", formattedNumberPassed, formattedNumberFailed));
//            conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.START);
//        });
    }

    public void onAllItemsCompleted() {
        SwingUtilities.invokeLater(() -> {
            conversionDTO.guiOrchestrator().setEnableSearchStepComponents(true);
            conversionDTO.convertComponent().getDropdownToggleButton().setEnabled(true);
        });

        conversionDTO.guiOrchestrator().hideProgressBar();
        conversionDTO.guiOrchestrator().onConversionDone();

        String coloredMessage = buildColoredFinishMessage("<span style='font-size:18pt; color:green; fond-weight:bold;'>Conversion completed</span>");

        DecimalFormat df = buildNumberFormatter();
        String formattedNumberPassed = df.format(passes.get());
        String formattedNumberFailed = df.format(failures.get());

        SwingUtilities.invokeLater(() -> {
            applicationStatusLabel.setText(coloredMessage);
            secondaryApplicationStatusLabel.setText(String.format("Conversion done! %s successes / %s failures", formattedNumberPassed, formattedNumberFailed));
            conversionDTO.convertComponent().updateAction(SplitButtonComponent.Action.START);
        });
    }


    private String buildColoredFinishMessage(String mainMessage) {
        int total = totalFound.get();
        int passed = passes.get();
        int failed = failures.get();
        int unprocessed = totalFound.get() - failed - passed;

        String colorPassed = passed > 0 ? "green" : "white";
        String colorUnprocessed = totalFound.get() - passed - failed > 0 ? "#CCCC00" : "white";
        String colorFailed = failed > 0 ? "red" : "white";

        DecimalFormat df = buildNumberFormatter();
        String formattedNumberPassed = df.format(passed);
        String formattedNumberFailed = df.format(failed);
        String formattedNumberTotal = df.format(total);
        String formattedNumberUnprocessed = df.format(unprocessed);

        return String.format("<html>" +
                "<div style='text-align:center;'>" +
                mainMessage +
                "<br/>" +
                "<br/>" +
                "<span style='font-size:14pt; font-weight:bold;'>Total: </span>" +
                "<span style='font-size:14pt; font-weight:bold;'>%s. </span>" +
                "<span style='color:" + colorPassed + "; font-weight:bold; font-size:14pt;'>Success: %s / </span>" +
                "<span style='color:" + colorUnprocessed + "; font-weight:bold; font-size:14pt;'>Unprocessed: %s / </span>" +
                "<span style='color:" + colorFailed + "; font-weight:bold; font-size:14pt;'>Failed: %s</span>" +
                "</div>" +
                "</html>", formattedNumberTotal, formattedNumberPassed, formattedNumberUnprocessed, formattedNumberFailed);
    }


    private void onConvertionAborted() {
        String message = buildColoredFinishMessage("<span style='font-size:18pt; color:red; fond-weight:bold;'>Conversion aborted by user</span>");
        SwingUtilities.invokeLater(() -> {
            applicationStatusLabel.setText(message);
            secondaryApplicationStatusLabel.setText("Convertion aborted");
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

    private void addToDeletionQueue(Path path) {
        globalDeletionQueue.add(path);
    }
}
