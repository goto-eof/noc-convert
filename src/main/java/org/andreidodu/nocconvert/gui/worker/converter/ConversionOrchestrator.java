package org.andreidodu.nocconvert.gui.worker.converter;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionOrchestratorInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertSingleItemTaskInputDTO;
import org.andreidodu.nocconvert.enums.NotificationLevel;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.util.FileUtil;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.andreidodu.nocconvert.util.FileUtil.buildVirtualThreadFactory;
import static org.andreidodu.nocconvert.util.OperationUtil.retryable;

public class ConversionOrchestrator {
    private static final Logger log = LogManager.getLogger(ConversionOrchestrator.class);
    public static final int MAX_NUM_ITEMS_FOR_NOTIFICATION_LEVEL_LOW = 10_000;
    private final Semaphore semaphore;
    @Getter
    private final ConversionOrchestratorInputDTO conversionOrchestratorInputDTO;
    //    @Getter
//    private Path tmpDirPath;
//    private Path finalDir;
    @Getter
    private final AtomicBoolean manualShutdown = new AtomicBoolean(false);

    private CountDownLatch finishLatch;
    private final ConcurrentLinkedQueue<ConversionItemDTO> processedFilesQueue = new ConcurrentLinkedQueue<>();

    public ConversionOrchestrator(ConversionOrchestratorInputDTO conversionOrchestratorInputDTO) {
        this.conversionOrchestratorInputDTO = conversionOrchestratorInputDTO;
        semaphore = new Semaphore(conversionOrchestratorInputDTO.virtualThreadsPermits());
    }


    public void startConversion() {
        try {
            if (!conversionOrchestratorInputDTO.temporaryDirectory().toFile().exists()) {
                log.debug("Creating temporary directory: {}", conversionOrchestratorInputDTO.temporaryDirectory());
                FileUtils.forceMkdirParent(conversionOrchestratorInputDTO.temporaryDirectory().toFile());
            }
            conversionOrchestratorInputDTO.addToDeletionQueue().accept(conversionOrchestratorInputDTO.temporaryDirectory());
            conversionOrchestratorInputDTO.conversionItemDTOList()
                    .forEach(dto -> dto.setDestinationDirectory(conversionOrchestratorInputDTO.finalDirectory()));
        } catch (NoSuchElementException e) {
            log.error("Conversion list is empty. Cannot start conversion.", e);
            throw new RuntimeException("Conversion list is empty. Cannot determine destination path.", e);
        } catch (IOException e) {
            log.error("Failed to build tmp dir for conversion orchestrator", e);
            throw new RuntimeException(e);
        }

        finishLatch = new CountDownLatch(conversionOrchestratorInputDTO.conversionItemDTOList().size());

        throwExceptionIfManuallyAborted();

        try {
            for (ConversionItemDTO conversionItemDTO : conversionOrchestratorInputDTO.conversionItemDTOList()) {
                throwExceptionIfManuallyAborted();
                semaphore.acquire();
                throwExceptionIfManuallyAborted();
                conversionItemDTO.setStatus(ConversionStatus.QUEUED);
                // conversionItemDTO.setIndex(index++);
                conversionItemDTO.setTmpDirectory(conversionOrchestratorInputDTO.temporaryDirectory());
                ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO = buildInput(conversionItemDTO, conversionOrchestratorInputDTO.temporaryDirectory());
                SingleItemConvertionTask task = new SingleItemConvertionTask(convertSingleItemTaskInputDTO);
                conversionOrchestratorInputDTO.virtualThreadsExecutor().submit(task);
            }
        } catch (InterruptedException | ConversionManualAbortedException e) {
            semaphore.drainPermits();
            while (finishLatch.getCount() > 0) {
                finishLatch.countDown();
            }
        }


        if (!manualShutdown.get()) {
            try {
                finishLatch.await();
                waitGentlyAndManageCompletion();
            } catch (InterruptedException e) {
                conversionOrchestratorInputDTO.onConversionAborted().run();
                log.error("Failed to complete conversion.", e);
            }
        }
    }

    private void throwExceptionIfManuallyAborted() {
        if (isThreadInterrupted() || manualShutdown.get()) {
            throw new ConversionManualAbortedException("manual abortion");
        }
    }


    private ConvertSingleItemTaskInputDTO buildInput(ConversionItemDTO conversionItemDTO, Path tmpDir) {
        return ConvertSingleItemTaskInputDTO.builder()
                .conversionItemDTO(conversionItemDTO)
                .tmpPath(tmpDir)
                .publishItemUpdate(conversionOrchestratorInputDTO.publishItemUpdate())
                .setItemAsCompleted(this::setItemAsCompletedOverride)
                .platformExecutorService(conversionOrchestratorInputDTO.platformThreadsExecutor())
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
        if (conversionOrchestratorInputDTO.conversionItemDTOList().size() > MAX_NUM_ITEMS_FOR_NOTIFICATION_LEVEL_LOW) {
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
            if (!manualShutdown.get()) {
                ThreadFactory virtualThreadFactory = buildVirtualThreadFactory("terminator");
                try (ExecutorService temporaryVirtualThreadExecutorService = Executors.newThreadPerTaskExecutor(virtualThreadFactory)) {
                    CompletableFuture<Void> futureResult =
                            CompletableFuture.supplyAsync(this::renameAllProcessedFiles, temporaryVirtualThreadExecutorService)
                                    .thenRunAsync(() -> conversionOrchestratorInputDTO.updateTmpDestinationDirectory().accept(this.retryableDirectoryRename(conversionOrchestratorInputDTO.temporaryDirectory(), conversionOrchestratorInputDTO.finalDirectory())), temporaryVirtualThreadExecutorService)
                                    .thenRunAsync(() -> conversionOrchestratorInputDTO.countDownLatchWorkFinished().countDown(), temporaryVirtualThreadExecutorService);
                    futureResult.join();
                }
                conversionOrchestratorInputDTO.onAllTasksCompleteEnableComponents().run();
                conversionOrchestratorInputDTO.countDownLatchWorkFinished().countDown();
            } else {
                conversionOrchestratorInputDTO.addToDeletionQueue().accept(conversionOrchestratorInputDTO.temporaryDirectory());
                conversionOrchestratorInputDTO.onConversionAborted().run();
            }
        } catch (Exception e) {
            conversionOrchestratorInputDTO.onConversionAborted().run();
            log.error("Failed to complete conversion.", e);
            conversionOrchestratorInputDTO.countDownLatchWorkFinished().countDown();
        }
    }


    private synchronized Path calculateTemporaryFilenameForMe(ConversionItemDTO convertImageInputDTO) {
        Path prototypeNewPathAndFilenameWithNewExtension = getPotentiallyDuplicateOutputRawFilenameWithNewExtension(convertImageInputDTO);
        return FileUtil.calculateNonExistingTemporaryOutputFilename(prototypeNewPathAndFilenameWithNewExtension);
    }

    private void copyMe(ConversionItemDTO conversionItemDTO) {
        try {
            Path sourceFile = conversionItemDTO.getSourceFile();
            Path uniqueTmpOutputFilenameWithNewExtension = conversionItemDTO.getTmpFile();
            Files.copy(sourceFile, uniqueTmpOutputFilenameWithNewExtension, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Unable to copy image file: {}", conversionItemDTO.getSourceFile(), e);
        }
    }

    private Void renameAllProcessedFiles() {
        while (!processedFilesQueue.isEmpty()) {
            ConversionItemDTO conversionItemDTO = processedFilesQueue.poll();

            if (ConversionStatus.COMPLETED.equals(conversionItemDTO.getStatus())) {
                Path uniqueTmpFilename = conversionItemDTO.getTmpFile();
                Path uniqueFinalFilename = FileUtil.tmpFilenameToFinal(uniqueTmpFilename);

                boolean result = retryable(() -> {
                    Files.move(uniqueTmpFilename, uniqueFinalFilename, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                });

                if (!result) {
                    log.error("Unable to rename image file: from {} to {}", uniqueTmpFilename, uniqueFinalFilename);
                }

            } else if (ConversionStatus.FAILED.equals(conversionItemDTO.getStatus())) {

                if (conversionItemDTO.getTmpFile().toFile().exists()) {
                    boolean result = conversionItemDTO.getTmpFile().toFile().delete();
                    log.debug("Delete image file: {} because it's status is FAILED, so I considered it as a corrupted file", result);
                }

                conversionOrchestratorInputDTO.incrementFailures();
                conversionOrchestratorInputDTO.decrementSuccesses();
            }
        }
        return null;
    }

    private Path getPotentiallyDuplicateOutputRawFilenameWithNewExtension(ConversionItemDTO conversionItemDTO) {
        return Path.of(conversionItemDTO.getTmpDirectory().toString(), FileUtil.removeFileExtension(conversionItemDTO.getSourceFile().getFileName().toString()) + "." + FileUtil.getExtension(conversionItemDTO.getTargetExtension()));
    }

    public void onAllTasksCompleteEnableComponents() {
        conversionOrchestratorInputDTO.onAllTasksCompleteEnableComponents().run();
        conversionOrchestratorInputDTO.mainSemaphore().release();
        //conversionOrchestratorInputDTO.countDownLatchWorkFinished().countDown();
    }

    private Path retryableDirectoryRename(Path sourcePath, Path destinationPath) {
        if (!sourcePath.getFileName().toString().contains("-tmp-")) {
            return sourcePath;
        }
        int i = 1;
        boolean retry;
        Path copyOfDestinationPath = destinationPath;
        do {
            try {
                FileUtils.moveDirectory(sourcePath.toFile(), copyOfDestinationPath.toFile());
                retry = false;
                return copyOfDestinationPath;
            } catch (IOException e) {
                copyOfDestinationPath = Path.of(destinationPath + "-" + (i++));
                retry = true;
            }
        } while (retry);
        return copyOfDestinationPath;
    }

    public void manualShutdown() {

        this.manualShutdown.set(true);

        semaphore.drainPermits();

        while (finishLatch.getCount() > 0) {
            finishLatch.countDown();
        }

        if (conversionOrchestratorInputDTO.platformThreadsExecutor() != null && !conversionOrchestratorInputDTO.platformThreadsExecutor().isTerminated()) {
            conversionOrchestratorInputDTO.platformThreadsExecutor().shutdownNow();
        }

        if (conversionOrchestratorInputDTO.virtualThreadsExecutor() != null && !conversionOrchestratorInputDTO.virtualThreadsExecutor().isTerminated()) {
            this.conversionOrchestratorInputDTO.virtualThreadsExecutor().shutdownNow();
        }
        waitGentlyAndManageCompletion();
    }

}
