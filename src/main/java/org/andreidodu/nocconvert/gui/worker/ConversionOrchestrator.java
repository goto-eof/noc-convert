package org.andreidodu.nocconvert.gui.worker;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionOrchestratorInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertSingleItemTaskInputDTO;
import org.andreidodu.nocconvert.enums.NotificationLevel;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.helper.FileUtil;
import org.andreidodu.nocconvert.helper.OSUtils;
import org.andreidodu.nocconvert.helper.performance.AdaptiveSimpleGovernorRunnable;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.andreidodu.nocconvert.constants.ApplicationConfig.FINAL_OUTPUT_DIRECTORY_NAME;
import static org.andreidodu.nocconvert.constants.ApplicationConfig.TMP_OUTPUT_DIRECTORY_NAME;
import static org.andreidodu.nocconvert.helper.FileUtil.buildVirtualThreadFactory;
import static org.andreidodu.nocconvert.helper.performance.AdaptiveSimpleGovernorRunnable.calculateSafeValueWithoutXPercent;

public class ConversionOrchestrator {
    private static final Logger log = LogManager.getLogger(ConversionOrchestrator.class);
    public static final int MAX_NUM_ITEMS_FOR_NOTIFICATION_LEVEL_LOW = 10_000;
    private final ExecutorService virtualThreadExecutor;
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
    private final AtomicBoolean manualShutdown = new AtomicBoolean(false);

    private CountDownLatch finishLatch;
    private final ConcurrentLinkedQueue<ConversionItemDTO> processedFilesQueue = new ConcurrentLinkedQueue<>();

    public ConversionOrchestrator(ConversionOrchestratorInputDTO conversionOrchestratorInputDTO) {
        this.conversionOrchestratorInputDTO = conversionOrchestratorInputDTO;

        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        platformThreadsPermits = permits;
        platformExecutorService = Executors.newFixedThreadPool(platformThreadsPermits);

        virtualThreadsPermits = calculateSafeValueWithoutXPercent(permits, 50);
        semaphore = new Semaphore(virtualThreadsPermits);
    }


    public void startConversion() {
        try {
            Path conversionDestinationDirectory = conversionOrchestratorInputDTO.conversionItemDTOList()
                    .stream()
                    .findFirst()
                    .orElseThrow()
                    .getDestinationDirectory();
            tmpDirPath = buildTmpPathAndReturn(conversionDestinationDirectory);

            finalDir = Path.of(conversionDestinationDirectory.toString(), FINAL_OUTPUT_DIRECTORY_NAME);
            conversionOrchestratorInputDTO.conversionItemDTOList()
                    .forEach(dto -> dto.setDestinationDirectory(finalDir));
            log.debug("Creating temporary directory: {}", tmpDirPath);
        } catch (NoSuchElementException e) {
            log.error("Conversion list is empty. Cannot start conversion.", e);
            throw new RuntimeException("Conversion list is empty. Cannot determine destination path.", e);
        } catch (IOException e) {
            log.error("Failed to build tmp dir for conversion orchestrator", e);
            throw new RuntimeException(e);
        }

        finishLatch = new CountDownLatch(conversionOrchestratorInputDTO.conversionItemDTOList().size());
        int index = 0;

        throwExceptionIfManuallyAborted();

        try {
            for (ConversionItemDTO conversionItemDTO : conversionOrchestratorInputDTO.conversionItemDTOList()) {
                SingleItemConvertionTask task = null;
                throwExceptionIfManuallyAborted();
                semaphore.acquire();
                throwExceptionIfManuallyAborted();
                conversionItemDTO.setStatus(ConversionStatus.QUEUED);
                conversionItemDTO.setIndex(index++);
                conversionItemDTO.setTmpDirectory(tmpDirPath);
                ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO = buildInput(conversionItemDTO, tmpDirPath);
                task = new SingleItemConvertionTask(convertSingleItemTaskInputDTO);
                virtualThreadExecutor.submit(task);
            }
        } catch (InterruptedException | ConversionManualAbortedException e) {
            semaphore.drainPermits();
            while (finishLatch.getCount() > 0) {
                finishLatch.countDown();
            }
        }
        if (!manualShutdown.get()) {
            waitGentlyAndManageCompletion();
        }
    }

    private void throwExceptionIfManuallyAborted() {
        if (isThreadInterrupted() || manualShutdown.get()) {
            throw new ConversionManualAbortedException("manual abortion");
        }
    }

    private static Path buildTmpPathAndReturn(Path tmpOutputDirectory) throws IOException {
        Path tmpDirPath = Path.of(tmpOutputDirectory.toString(), TMP_OUTPUT_DIRECTORY_NAME + "-" + UUID.randomUUID());
        Files.createDirectories(tmpDirPath);
        return tmpDirPath;
    }

    private ConvertSingleItemTaskInputDTO buildInput(ConversionItemDTO conversionItemDTO, Path tmpDir) {
        return ConvertSingleItemTaskInputDTO.builder()
                .conversionItemDTO(conversionItemDTO)
                .tmpPath(tmpDir)
                .publishItemUpdate(conversionOrchestratorInputDTO.publishItemUpdate())
                .setItemAsCompleted(this::setItemAsCompletedOverride)
                .platformExecutorService(platformExecutorService)
                .incrementFailures(conversionOrchestratorInputDTO.incrementFailures())
                .incrementPasses(conversionOrchestratorInputDTO.incrementPasses())
                .isParentInterrupted(this::isThreadInterrupted)
                .notificationLevel(calculateNotificationLevel())
                .copyMe(this::copyMe)
                .semaphore(semaphore)
                .countFinish(finishLatch)
                .calculateTemporaryFilenameForMe(this::calculateTemporaryFilenameForMe)
                .readerFormatNames(Arrays.stream(ImageIO.getReaderFormatNames()).toList())
                .processedFilesQueue(this::addToProcessedFilesQueue)
                .build();
    }

    private NotificationLevel calculateNotificationLevel() {
        if (conversionOrchestratorInputDTO.conversionItemDTOList().size() >
                MAX_NUM_ITEMS_FOR_NOTIFICATION_LEVEL_LOW) {
            return NotificationLevel.LOW;
        }
        return NotificationLevel.HIGH;
    }

    private Boolean isThreadInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    private void addToProcessedFilesQueue(ConversionItemDTO conversionItemDTO) {
        processedFilesQueue.add(conversionItemDTO);
    }

    private void setItemAsCompletedOverride(ConversionItemDTO conversionItemDTO) {
        conversionOrchestratorInputDTO.setItemAsCompleted().accept(conversionItemDTO);
    }

    private void waitGentlyAndManageCompletion() {
        try {

            finishLatch.await();

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
                ThreadFactory virtualThreadFactory = buildVirtualThreadFactory("terminator");
                try (ExecutorService temporaryVirtualThreadExecutorService = Executors.newThreadPerTaskExecutor(virtualThreadFactory)) {
                    CompletableFuture<Void> futureResult = CompletableFuture.supplyAsync(this::renameAllProcessedFiles, temporaryVirtualThreadExecutorService)
                            .thenRunAsync(() -> this.retryableDirectoryRename(tmpDirPath, finalDir), temporaryVirtualThreadExecutorService);
                    futureResult.join();
                }
                conversionOrchestratorInputDTO.onAllTasksCompleteEnableComponents().run();
            } else {
                // (Thread.ofVirtual().start(this::deleteTemporaryDirectoryIfExists);
                conversionOrchestratorInputDTO.addToDeletionQueue().accept(tmpDirPath);
                conversionOrchestratorInputDTO.onConversionAborted().run();
            }

        } catch (Exception e) {
            conversionOrchestratorInputDTO.onConversionAborted().run();
            log.error("Failed to complete conversion.", e);
        }
    }


    private void waitForCompletionAndShutdownGentlyVirtualThreadExecutor() throws InterruptedException {
        virtualThreadExecutor.shutdown();
        boolean terminated = virtualThreadExecutor.awaitTermination(31, TimeUnit.DAYS);
        if (!terminated) {
            log.error("virtualThreadExecutor did not terminate in time. Forcing emergency shutdown.");
            virtualThreadExecutor.shutdownNow();
        }
    }

    private void waitForCompletionAndShutdownGentlyPlatformThreadExecutor() throws InterruptedException {
        platformExecutorService.shutdown();
        boolean platformExecutorShutdown = platformExecutorService.awaitTermination(31, TimeUnit.DAYS);
        if (!platformExecutorShutdown) {
            log.error("platformExecutorService did not terminate in time. Forcing emergencyConversionWorker shutdown.");
            platformExecutorService.shutdownNow();
        }
    }

    private synchronized Path calculateTemporaryFilenameForMe(ConversionItemDTO convertImageInputDTO) {
        Path prototypeNewPathAndFilenameWithNewExtension = getPotentiallyDuplicateOutputRawFilenameWithNewExtension(convertImageInputDTO);
        return FileUtil.calculateNonExistingTemporaryOutputFilename(prototypeNewPathAndFilenameWithNewExtension);
    }

    private void copyMe(ConversionItemDTO conversionItemDTO) {
        //synchronized (COPY_RENAME_LOCK) {
//            Path sourceFile = conversionItemDTO.sourceFile();
//            Path potentiallyDuplicateOutputFilenameWithNewExtension = getPotentiallyDuplicateOutputRawFilenameWithNewExtension(conversionItemDTO);
        try {
//                if (potentiallyDuplicateOutputFilenameWithNewExtension.toFile().exists()) {
//                    potentiallyDuplicateOutputFilenameWithNewExtension = FileUtil.calculateNonExistingOutputFilename(potentiallyDuplicateOutputFilenameWithNewExtension, conversionItemDTO.targetExtension());
//                }
//                doublecheckExistenceAndThrowExceptionIfNecessary(potentiallyDuplicateOutputFilenameWithNewExtension);
            Path sourceFile = conversionItemDTO.getSourceFile();
            Path uniqueTmpOutputFilenameWithNewExtension = conversionItemDTO.getTmpFile();
            Files.copy(sourceFile, uniqueTmpOutputFilenameWithNewExtension, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Unable to copy image file: {}", conversionItemDTO.getSourceFile(), e);
        }
        //}
    }

//    private void renameMe(Path existingTemporaryFile, ConvertImageInputDTO convertImageInputDTO) {
//        synchronized (COPY_RENAME_LOCK) {
//            Path potentiallyDuplicateOutputFilenameWithNewExtension = getPotentiallyDuplicateOutputRawFilenameWithNewExtension(convertImageInputDTO);
////            try {
//            if (potentiallyDuplicateOutputFilenameWithNewExtension.toFile().exists()) {
//                potentiallyDuplicateOutputFilenameWithNewExtension = FileUtil.calculateNonExistingOutputFilename(potentiallyDuplicateOutputFilenameWithNewExtension, convertImageInputDTO.targetExtension());
//            }
//            doublecheckExistenceAndThrowExceptionIfNecessary(potentiallyDuplicateOutputFilenameWithNewExtension);
//            existingTemporaryFile.toFile().renameTo(potentiallyDuplicateOutputFilenameWithNewExtension.toFile());
//            // FileUtils.moveFile(existingTemporaryFile.toFile(), potentiallyDuplicateOutputFilenameWithNewExtension.toFile(), StandardCopyOption.REPLACE_EXISTING);

    ////            } catch (IOException e) {
    ////                log.error("Unable to rename image file from {} to {}.\nError: {}", existingTemporaryFile.toFile(), potentiallyDuplicateOutputFilenameWithNewExtension, e.getMessage());
    ////            }
//        }
//    }
    private Void renameAllProcessedFiles() {
        processedFilesQueue.forEach(conversionItemDTO -> {
            if (ConversionStatus.COMPLETED.equals(conversionItemDTO.getStatus())) {
                Path uniqueTmpFilename = conversionItemDTO.getTmpFile();
                Path uniqueFinalFilename = FileUtil.tmpFilenameToFinal(uniqueTmpFilename);
                try {
                    if (!uniqueTmpFilename.toFile().renameTo(uniqueFinalFilename.toFile())) {
                        throw new IOException("Unable to rename image file: " + uniqueTmpFilename);
                    }
                } catch (IOException e) {
                    log.error("Unable to rename image file: from {} to {}", uniqueTmpFilename, uniqueFinalFilename, e);
                }
            } else if (ConversionStatus.FAILED.equals(conversionItemDTO.getStatus())) {
                if (conversionItemDTO.getTmpFile().toFile().delete()) {
                }
                conversionOrchestratorInputDTO.incrementFailures();
                conversionOrchestratorInputDTO.decrementSuccesses();
            }
        });
        return null;
    }

    private Path getPotentiallyDuplicateOutputRawFilenameWithNewExtension(ConversionItemDTO conversionItemDTO) {
        // ex. filename.ico
        return Path.of(conversionItemDTO.getTmpDirectory().toString(), FileUtil.removeFileExtension(conversionItemDTO.getSourceFile().getFileName().toString()) + "." + FileUtil.getExtension(conversionItemDTO.getTargetExtension()));
    }

    private Path removeTmpExtensionAndUUIDPrefix(Path path) {
        // ex. filename.ico.tmp ->  filename.ico
        return Path.of(path.getParent().toString(), FileUtil.removeFileExtension(path.getFileName().toString()));
    }

    private static void doublecheckExistenceAndThrowExceptionIfNecessary(Path uniqueOutputFilename) {
        if (uniqueOutputFilename.toFile().exists()) {
            throw new RuntimeException("File already exists: " + uniqueOutputFilename.toFile().getAbsolutePath());
        }
    }

    public void onAllTasksCompleteEnableComponents() {
        conversionOrchestratorInputDTO.onAllTasksCompleteEnableComponents().run();
    }

    private Void retryableDirectoryRename(Path sourcePath, Path destinationPath) {
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
        return null;
    }

    public void manualShutdown() {

        this.manualShutdown.set(true);

        semaphore.drainPermits();

        while (finishLatch.getCount() > 0) {
            finishLatch.countDown();
        }

        if (platformExecutorService != null && !platformExecutorService.isTerminated()) {
            platformExecutorService.shutdownNow();
        }

        if (virtualThreadExecutor != null && !virtualThreadExecutor.isTerminated()) {
            this.virtualThreadExecutor.shutdownNow();
        }

        waitGentlyAndManageCompletion();


    }

    private void deleteTemporaryDirectoryIfExists() {
        try {
            if (OSUtils.isLinux() || OSUtils.isMacOS()) {
                deleteTemporaryDirectoryOnUnixLikeIfExists();
                return;
            }

            FileUtils.deleteDirectory(tmpDirPath.toFile());
        } catch (IOException e) {
            log.error("unable to delete temporary directory {}", tmpDirPath, e);
        }
    }

    private void deleteTemporaryDirectoryOnUnixLikeIfExists() {
        try {
            log.warn("Starting deletion (rm -rf) for: {}", tmpDirPath);

            ProcessBuilder pb = new ProcessBuilder("rm", "-rf", tmpDirPath.toString());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            int exitCode = p.waitFor();

            if (exitCode == 0) {
                log.warn("Deletion completed with success: {}", tmpDirPath);
            } else {
                log.error("rm -rf failed with exit code {}. Fallback to FileUtils.deleteDirectory().", exitCode);
                FileUtils.deleteDirectory(tmpDirPath.toFile());
                log.warn("Fallback to FileUtils.deleteDirectory() completed.");
            }
        } catch (IOException | InterruptedException e) {
            log.error("Unable to delete tmp directory with rm -rf {}. Error: {}", tmpDirPath, e.getMessage());
            try {
                log.error("Fallback to FileUtils.deleteDirectory().");
                FileUtils.deleteDirectory(tmpDirPath.toFile());
                log.warn("Fallback to FileUtils.deleteDirectory() completed.");
            } catch (IOException fallbackE) {
                log.error("Also classic faillback  failed for {}. Error message: {}", tmpDirPath, fallbackE.getMessage());
            }
        }
    }

}
