package org.andreidodu.nocconvert.gui.worker;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionOrchestratorInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertSingleItemTaskInputDTO;
import org.andreidodu.nocconvert.enums.NotificationLevel;
import org.andreidodu.nocconvert.helper.FileUtil;
import org.andreidodu.nocconvert.helper.performance.AdaptiveSimpleGovernorRunnable;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.andreidodu.nocconvert.helper.performance.AdaptiveSimpleGovernorRunnable.calculateSafeValueWithoutXPercent;

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
    @Getter
    private Path tmpDirPath;
    private Path finalDir;
    @Getter
    private final ConcurrentLinkedQueue<Path> filesQueue = new ConcurrentLinkedQueue<>();
    private final String targetExtension;
    private final AtomicBoolean manualShutdown = new AtomicBoolean(false);
    public static final String FINAL_OUTPUT_DIRECTORY_NAME = "noc-convert";
    public static final String TMP_OUTPUT_DIRECTORY_NAME = "noc-convert-tmp";

    private final static Object COPY_RENAME_LOCK = new Object();

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
            finalDir = Path.of(conversionDestinationDirectory.toString(), FINAL_OUTPUT_DIRECTORY_NAME);
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

        if (!this.manualShutdown.get()) {
            onCompleteAllWithSuccess();
        }

    }

    private static Path buildTmpPathAndReturn(Path tmpOutputDirectory) throws IOException {
        Path tmpDirPath = Path.of(tmpOutputDirectory.toString(), TMP_OUTPUT_DIRECTORY_NAME);
        Files.createDirectories(tmpDirPath);
        return tmpDirPath;
    }

//    private static Path buildParentTmpPathAndReturn(Path tmpOutputDirectory) throws IOException {
//        Path tmpDirPath = Path.of(tmpOutputDirectory.toString(), "noc-convert");
//        Files.createDirectories(tmpDirPath);
//        return tmpDirPath;
//    }

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
                .renameMe(this::renameMe)
                .copyMe(this::copyMe)
                .calculateTemporaryFilenameForMe(this::calculateTemporaryFilenameForMe)
                .readerFormatNames(Arrays.stream(ImageIO.getReaderFormatNames()).toList())
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

    private void onCompleteAllWithSuccess() {
        try {

            boolean rename = true;

            try {
                waitForCompletionAndShutdownGentlyVirtualThreadExecutor();
            } catch (InterruptedException e) {
                log.debug("Interrupted while waiting for virtualThreadExecutor to terminate.");
                rename = false;
            }

            try {
                waitForCompletionAndShutdownGentlyPlatformThreadExecutor();
            } catch (InterruptedException e) {
                log.debug("Interrupted while waiting for platformExecutorService to terminate.");
                rename = false;
            }

            if (rename && !manualShutdown.get()) {
                retryableDirectoryRename(tmpDirPath, finalDir);
            } else {
                deleteTemporaryDirectoryIfExists();
                conversionOrchestratorInputDTO.onConversionAborted().run();
            }

        } finally {
            conversionOrchestratorInputDTO.onAllTasksCompleteEnableComponents().run();
        }
    }

    private void waitForCompletionAndShutdownGentlyVirtualThreadExecutor() throws InterruptedException {
        virtualThreadExecutor.shutdown();
        boolean terminated = virtualThreadExecutor.awaitTermination(31, TimeUnit.DAYS);
        virtualTaskList.forEach(SingleItemConvertionTask::closeStreams);
        if (!terminated) {
            log.error("virtualThreadExecutor did not terminate in time. Forcing emergency shutdown.");
            virtualThreadExecutor.shutdownNow();
        }
    }

    private void waitForCompletionAndShutdownGentlyPlatformThreadExecutor() throws InterruptedException {
        platformExecutorService.shutdown();
        boolean platformExecutorShutdown = platformExecutorService.awaitTermination(31, TimeUnit.DAYS);
        virtualTaskList.forEach(SingleItemConvertionTask::closeStreams);
        if (!platformExecutorShutdown) {
            log.error("platformExecutorService did not terminate in time. Forcing emergencyConversionWorker shutdown.");
            platformExecutorService.shutdownNow();
        }
    }

//    private void renameTmpDirectory() {
//        boolean result = FileUtil.renameDirectory(tmpDirPath, finalDir);
//        if (!result) {
//            log.warn("Unable to rename the directory");
//        }
//    }

    private synchronized Path calculateTemporaryFilenameForMe(ConvertImageInputDTO convertImageInputDTO) {
        Path sourceFile = convertImageInputDTO.sourceFile();
        Path potentiallyDuplicateOutputFilenameWithNewExtension = getPotentiallyDuplicateOutputFilenameWithNewExtension(convertImageInputDTO, sourceFile);
        return FileUtil.calculateNonExistingTemporaryOutputFilename(potentiallyDuplicateOutputFilenameWithNewExtension, convertImageInputDTO.targetExtension());
    }

    private void copyMe(ConvertImageInputDTO convertImageInputDTO) {
        synchronized (COPY_RENAME_LOCK) {
            Path sourceFile = convertImageInputDTO.sourceFile();
            Path potentiallyDuplicateOutputFilenameWithNewExtension = getPotentiallyDuplicateOutputFilenameWithNewExtension(convertImageInputDTO, sourceFile);
            Path uniqueOutputFilename = FileUtil.calculateNonExistingOutputFilename(potentiallyDuplicateOutputFilenameWithNewExtension, convertImageInputDTO.targetExtension(), false);
            try {
                doublecheckExistenceAndThrowExceptionIfNecessary(uniqueOutputFilename);
                Files.copy(sourceFile, uniqueOutputFilename, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Unable to copy image file: {}", convertImageInputDTO.sourceFile(), e);
            }
        }
    }

    private void renameMe(Path existingTemporaryFile, ConvertImageInputDTO convertImageInputDTO) {
        synchronized (COPY_RENAME_LOCK) {
            Path sourceFile = convertImageInputDTO.sourceFile();
            Path potentiallyDuplicateOutputFilenameWithNewExtension = getPotentiallyDuplicateOutputFilenameWithNewExtension(convertImageInputDTO, sourceFile);
            Path uniqueOutputFilename = FileUtil.calculateNonExistingOutputFilename(potentiallyDuplicateOutputFilenameWithNewExtension, convertImageInputDTO.targetExtension(), false);
            try {
                doublecheckExistenceAndThrowExceptionIfNecessary(uniqueOutputFilename);
                FileUtils.moveFile(existingTemporaryFile.toFile(), uniqueOutputFilename.toFile(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Unable to rename image file from {} to {}.\nError: {}", existingTemporaryFile.toFile(), uniqueOutputFilename, e.getMessage());
            }
        }
    }

    private Path getPotentiallyDuplicateOutputFilenameWithNewExtension(ConvertImageInputDTO convertImageInputDTO, Path sourceFile) {
        return Path.of(convertImageInputDTO.tmpPath().toString(), FileUtil.removeFileExtension(sourceFile.getFileName().toString()) + "." + FileUtil.getExtension(convertImageInputDTO.targetExtension()));
    }

    private static void doublecheckExistenceAndThrowExceptionIfNecessary(Path uniqueOutputFilename) {
        if (uniqueOutputFilename.toFile().exists()) {
            throw new RuntimeException("File already exists: " + uniqueOutputFilename.toFile().getAbsolutePath());
        }
    }

    public void onAllTasksCompleteEnableComponents() {
        conversionOrchestratorInputDTO.onAllTasksCompleteEnableComponents().run();
    }

    private void retryableDirectoryRename(Path sourcePath, Path destinationPath) {
        int i = 1;
        boolean retry;
        Path copyOfDestinationPath = destinationPath;
        do {
            try {
                FileUtils.moveDirectory(sourcePath.toFile(), copyOfDestinationPath.toFile());
                retry = false;
            } catch (IOException e) {
                copyOfDestinationPath = Path.of(destinationPath + "-" + (i++));
                retry = true;
            }
        } while (retry);
    }

    public void manualShutdown() {

        this.manualShutdown.set(true);

        virtualTaskList.forEach(SingleItemConvertionTask::closeStreams);

        if (platformExecutorService != null && !platformExecutorService.isTerminated()) {
            platformExecutorService.shutdownNow();
        }

        if (virtualThreadExecutor != null && !virtualThreadExecutor.isTerminated()) {
            this.virtualThreadExecutor.shutdownNow();
        }

    }

    private void deleteTemporaryDirectoryIfExists() {
        try {
            FileUtils.deleteDirectory(tmpDirPath.toFile());
        } catch (IOException e) {
            log.error("unable to delete temporary directory {}", tmpDirPath, e);
        }
    }

}
