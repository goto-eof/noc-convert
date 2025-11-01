package org.andreidodu.nocconvert.gui.worker;

import lombok.Getter;
import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionOrchestratorInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertSingleItemTaskInputDTO;
import org.andreidodu.nocconvert.util.FileUtil;
import org.andreidodu.nocconvert.util.performance.AdaptiveSimpleGovernorRunnable;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.andreidodu.nocconvert.util.performance.AdaptiveSimpleGovernorRunnable.calculateSafeValueWithoutXPercent;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ConversionOrchestrator {
    private static final Logger log = LogManager.getLogger(ConversionOrchestrator.class);
    private final ExecutorService virtualThreadExecutor;
    private List<ConvertSingleItemTask> virtualTaskList;
    private static Semaphore semaphore;
    private final ExecutorService platformExecutorService;
    @Getter
    private final int platformThreadsPermits;
    @Getter
    private final int virtualThreadsPermits;
    @Getter
    private final int permits = AdaptiveSimpleGovernorRunnable.IDEAL_NUM_OF_THREADS.get();
    private final ConversionOrchestratorInputDTO conversionOrchestratorInputDTO;
    private Path tmpDirPath;
    private Path tmpParentDirPath;
    private final ConcurrentLinkedQueue<Path> fileRenameQueue = new ConcurrentLinkedQueue<>();

    public ConversionOrchestrator(ConversionOrchestratorInputDTO conversionOrchestratorInputDTO) {
        this.conversionOrchestratorInputDTO = conversionOrchestratorInputDTO;

        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        platformThreadsPermits = permits;
        platformExecutorService = Executors.newFixedThreadPool(platformThreadsPermits);

        virtualThreadsPermits = calculateSafeValueWithoutXPercent(permits, 50);
        semaphore = new Semaphore(virtualThreadsPermits);
    }


    public void startConversion() {
        virtualTaskList = new ArrayList<>();
        try {
            Path conversionDestinationDirectory = conversionOrchestratorInputDTO.conversionItemDTOList().stream().findFirst().get().getDestinationDirectory();
            tmpDirPath = buildTmpPathAndReturn(conversionDestinationDirectory);
            tmpParentDirPath = buildParentTmpPathAndReturn(conversionDestinationDirectory);
            log.debug("Creating temporary directory: {}", tmpDirPath);
        } catch (IOException e) {
            log.error("Failed to build tmp dir for conversion orchestrator", e);
            throw new RuntimeException(e);
        }
        for (ConversionItemDTO conversionItemDTO : conversionOrchestratorInputDTO.conversionItemDTOList()) {
            conversionItemDTO.setStatus(ConversionStatus.QUEUED);
            ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO = buildInput(conversionItemDTO, tmpDirPath);
            ConvertSingleItemTask task = new ConvertSingleItemTask(convertSingleItemTaskInputDTO);
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
                .addToFileRenameQueue(this::addToFileRenameQueue)
                .build();
    }

    private void addToFileRenameQueue(Path file) {
        fileRenameQueue.add(file);
    }

    private void setItemAsCompletedOverride(ConversionItemDTO conversionItemDTO) {
        conversionOrchestratorInputDTO.setItemAsCompleted().accept(conversionItemDTO);
    }

    private void onCompleteAll() {
        try {
            virtualThreadExecutor.shutdown();
            boolean terminated = virtualThreadExecutor.awaitTermination(31, TimeUnit.DAYS);
            if (!terminated) {
                log.error("virtualThreadExecutor did not terminate in time. Forcing emergency shutdown.");
                virtualTaskList.forEach(ConvertSingleItemTask::closeStreams);
            }

            platformExecutorService.shutdown();
            boolean platformExecutorShutdown = platformExecutorService.awaitTermination(31, TimeUnit.DAYS);
            if (!platformExecutorShutdown) {
                log.error("platformExecutorService did not terminate in time. Forcing emergency shutdown.");
                platformExecutorService.shutdownNow();
            }

            virtualThreadExecutor.shutdownNow();

            if (!ApplicationConfig.DEV_MODE && tmpParentDirPath != null) {
                try {
                    FileUtils.deleteDirectory(tmpParentDirPath.toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            renameFiles();

        } catch (InterruptedException e) {
            log.warn("Conversion Worker interrupted (user STOP action). Forcing Orchestrator cancel.", e);
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
            onAllTasksComplete();
        }
    }

    private void renameFiles() {
        for (Path file : fileRenameQueue) {
            Path cleanFileName = FileUtil.getCleanFileNameWithoutExtension(file);
            String backupCleanFileName = cleanFileName.getFileName().toString();
            int i = 1;
            while (Files.exists(cleanFileName)) {
                cleanFileName = calculateNewName(cleanFileName, i, backupCleanFileName);
                i++;
            }
            try {
                Files.move(file, cleanFileName, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error(getRootCauseMessage(e), e);
            }
        }
    }

    private static Path calculateNewName(Path cleanFilename, int i, String cleanedFilename) {
        return Path.of(cleanFilename.getParent().toString(), "duplicate-" + i + "-of-" + cleanedFilename);
    }

    public void onAllTasksComplete() {
        conversionOrchestratorInputDTO.onAllTasksComplete().run();
    }

    public void shutdown() {

        if (platformExecutorService != null && !platformExecutorService.isTerminated()) {
            platformExecutorService.shutdownNow();
        }

        if (virtualThreadExecutor != null && !virtualThreadExecutor.isTerminated()) {
            virtualTaskList.forEach(ConvertSingleItemTask::closeStreams);
            this.virtualThreadExecutor.shutdownNow();
        }

    }
}
