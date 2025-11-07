package org.andreidodu.nocconvert.gui.worker;

import lombok.Getter;
import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionOrchestratorInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertSingleItemTaskInputDTO;
import org.andreidodu.nocconvert.enums.NotificationLevel;
import org.andreidodu.nocconvert.util.FileUtil;
import org.andreidodu.nocconvert.util.performance.AdaptiveSimpleGovernorRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;

import static org.andreidodu.nocconvert.util.OperationUtil.retryable;
import static org.andreidodu.nocconvert.util.performance.AdaptiveSimpleGovernorRunnable.calculateSafeValueWithoutXPercent;

public class ConversionOrchestrator {
    private static final Logger log = LogManager.getLogger(ConversionOrchestrator.class);
    public static final int RENAME_SEMAPHORE_SIZE = 100;
    public static final int MAX_NUM_ITEMS_FOR_NOTIFICATION_LEVEL_LOW = 10_000;
    private final ExecutorService virtualThreadExecutor;
    private List<SingleItemConvertionTask> virtualTaskList;
    private final Semaphore semaphore;
    private final ExecutorService platformExecutorService;
    @Getter
    private final int platformThreadsPermits;
    @Getter
    private final int virtualThreadsPermits;
    @Getter
    private final int permits = AdaptiveSimpleGovernorRunnable.IDEAL_NUM_OF_THREADS.get();
    private final ConversionOrchestratorInputDTO conversionOrchestratorInputDTO;
    private Path tmpDirPath;
    @Getter
    private Path tmpParentDirPath;
    @Getter
    private final ConcurrentLinkedQueue<Path> filesQueue = new ConcurrentLinkedQueue<>();
    private final String targetExtension;

    public ConversionOrchestrator(ConversionOrchestratorInputDTO conversionOrchestratorInputDTO) {
        this.conversionOrchestratorInputDTO = conversionOrchestratorInputDTO;
        targetExtension = conversionOrchestratorInputDTO.conversionItemDTOList().stream().findFirst().orElseThrow().getTargetExtension();

        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        platformThreadsPermits = permits;
        platformExecutorService = Executors.newFixedThreadPool(platformThreadsPermits);

        virtualThreadsPermits = calculateSafeValueWithoutXPercent(permits, 50);
        semaphore = new Semaphore(virtualThreadsPermits);
    }


    public void startConversion() {
        virtualTaskList = new ArrayList<>();
        try {
            Path conversionDestinationDirectory = conversionOrchestratorInputDTO.conversionItemDTOList()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .getDestinationDirectory();
            tmpDirPath = buildTmpPathAndReturn(conversionDestinationDirectory);
            tmpParentDirPath = buildParentTmpPathAndReturn(conversionDestinationDirectory);
            log.debug("Creating temporary directory: {}", tmpDirPath);
        } catch (NoSuchElementException e) {
            log.error("Conversion list is empty. Cannot start conversion.", e);
            throw new RuntimeException("Conversion list is empty. Cannot determine destination path.", e);
        } catch (IOException e) {
            log.error("Failed to build tmp dir for conversion orchestrator", e);
            throw new RuntimeException(e);
        }
        int index = 0;
        for (ConversionItemDTO conversionItemDTO : conversionOrchestratorInputDTO.conversionItemDTOList()) {
            conversionItemDTO.setStatus(ConversionStatus.QUEUED);
            conversionItemDTO.setIndex(index++);
            ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO = buildInput(conversionItemDTO, tmpDirPath);
            SingleItemConvertionTask task = new SingleItemConvertionTask(convertSingleItemTaskInputDTO);
            virtualTaskList.add(task);
            virtualThreadExecutor.submit(task);
        }
        onCompleteAll();
    }


    private static Path buildTmpPathAndReturn(Path tmpOutputDirectory) throws IOException {
        Path tmpDirPath = Path.of(tmpOutputDirectory.toString(), "tmp", "attempt - " + System.currentTimeMillis());
        Files.createDirectories(tmpDirPath);
        return tmpDirPath;
    }

    private static Path buildParentTmpPathAndReturn(Path tmpOutputDirectory) throws IOException {
        Path tmpDirPath = Path.of(tmpOutputDirectory.toString(), "tmp");
        Files.createDirectories(tmpDirPath);
        return tmpDirPath;
    }

    private ConvertSingleItemTaskInputDTO buildInput(ConversionItemDTO conversionItemDTO, Path tmpDir) {
        return ConvertSingleItemTaskInputDTO.builder()
                .conversionItemDTO(conversionItemDTO)
                .tmpPath(tmpDir)
                .publishItemUpdate(conversionOrchestratorInputDTO.publishItemUpdate())
                .setItemAsCompleted(this::setItemAsCompletedOverride)
                .semaphore(semaphore)
                .platformExecutorService(platformExecutorService)
                .incrementFailures(conversionOrchestratorInputDTO.incrementFailures())
                .incrementPasses(conversionOrchestratorInputDTO.incrementPasses())
                .addToFileQueue(this::addToFileQueue)
                .isParentInterrupted(this::isInterrupted)
                .notificationLevel(calculateNotificationLevel())
                .build();
    }

    private NotificationLevel calculateNotificationLevel() {
        if (conversionOrchestratorInputDTO.conversionItemDTOList().size() >
                MAX_NUM_ITEMS_FOR_NOTIFICATION_LEVEL_LOW) {
            return NotificationLevel.LOW;
        }
        return NotificationLevel.HIGH;
    }

    private Boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    private void addToFileQueue(Path file) {
        filesQueue.add(file);
    }

    private void setItemAsCompletedOverride(ConversionItemDTO conversionItemDTO) {
        conversionOrchestratorInputDTO.setItemAsCompleted().accept(conversionItemDTO);
    }

    private void onCompleteAll() {
        try {


            boolean rename = true;

            try {
                shutdownVirtualThreadExecutor();
            } catch (InterruptedException e) {
                log.debug("Interrupted while waiting for virtualThreadExecutor to terminate.");
                rename = false;
            }

            try {
                shutdownPlatformThreadExecutor();
            } catch (InterruptedException e) {
                log.debug("Interrupted while waiting for platformExecutorService to terminate.");
                rename = false;
            }

            virtualTaskList.forEach(SingleItemConvertionTask::closeStreams);

            if (rename) {
                renameFiles();
            }

        } finally {
            shutdown();
            onAllTasksCompleteEnableComponents();
        }
    }

    private void shutdownVirtualThreadExecutor() throws InterruptedException {
        virtualThreadExecutor.shutdown();
        boolean terminated = virtualThreadExecutor.awaitTermination(31, TimeUnit.DAYS);
        virtualTaskList.forEach(SingleItemConvertionTask::closeStreams);
        if (!terminated) {
            log.error("virtualThreadExecutor did not terminate in time. Forcing emergency shutdown.");
            virtualThreadExecutor.shutdownNow();
        }
    }

    private void shutdownPlatformThreadExecutor() throws InterruptedException {
        platformExecutorService.shutdown();
        boolean platformExecutorShutdown = platformExecutorService.awaitTermination(31, TimeUnit.DAYS);
        virtualTaskList.forEach(SingleItemConvertionTask::closeStreams);
        if (!platformExecutorShutdown) {
            log.error("platformExecutorService did not terminate in time. Forcing emergencyConversionWorker shutdown.");
            platformExecutorService.shutdownNow();
        }
    }

    private void renameFiles() {
        renameFilesOnNewThread();
        deleteTmpDirectoryOnNewThread();
    }

    private void renameFilesOnNewThread() {
        filesQueue.forEach(filePath -> {
            if (filePath.toFile().exists()) {
                FileUtil.renameFile(filePath, targetExtension);
            }
        });
    }

    private void deleteTmpDirectoryOnNewThread() {
        if (!ApplicationConfig.DEV_MODE && tmpParentDirPath != null) {
            Thread.ofVirtual().start(() -> {
                sleep(1000);
                retryable(null, tmpDirPath, FileUtil::deleteDirectoryIfExists);
            });
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void onAllTasksCompleteEnableComponents() {
        conversionOrchestratorInputDTO.onAllTasksCompleteEnableComponents().run();
    }

    public void shutdown() {

        if (platformExecutorService != null && !platformExecutorService.isTerminated()) {
            platformExecutorService.shutdownNow();
        }

        if (virtualThreadExecutor != null && !virtualThreadExecutor.isTerminated()) {
            this.virtualThreadExecutor.shutdownNow();
        }

    }

}
