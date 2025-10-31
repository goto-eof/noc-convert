package org.andreidodu.nocconvert.gui.worker;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.util.performance.AdaptiveSimpleGovernorRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.andreidodu.nocconvert.util.performance.AdaptiveSimpleGovernorRunnable.calculateSafeValueWithoutXPercent;

public class ConversionOrchestrator {
    private static final Logger log = LogManager.getLogger(ConversionOrchestrator.class);
    private final Consumer<ConversionItemDTO> publishItemUpdate;
    private final Consumer<ConversionItemDTO> setItemAsCompleted;
    private final ExecutorService virtualThreadExecutor;
    private final List<ConversionItemDTO> conversionItemDTOList;
    private final List<ConversionItemDTO> updatedConversionItemDTOList = new CopyOnWriteArrayList<>();
    private List<ConvertSingleItemTask> virtualTaskList;
    private static Semaphore semaphore;
    private final Consumer<List<ConversionItemDTO>> onAllTasksComplete;
    private final ExecutorService platformExecutorService;
    @Getter
    private final int platformThreadsPermits;
    @Getter
    private final int virtualThreadsPermits;
    @Getter
    private final int permits = AdaptiveSimpleGovernorRunnable.IDEAL_NUM_OF_THREADS.get();

    public ConversionOrchestrator(
            List<ConversionItemDTO> conversionItemDTOList,
            Consumer<ConversionItemDTO> publishItemUpdate,
            Consumer<ConversionItemDTO> setItemAsCompleted,
            Consumer<List<ConversionItemDTO>> onAllTasksComplete
    ) {

        this.publishItemUpdate = publishItemUpdate;
        this.setItemAsCompleted = setItemAsCompleted;
        this.onAllTasksComplete = onAllTasksComplete;
        this.conversionItemDTOList = conversionItemDTOList;
        virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

        platformThreadsPermits = permits;
        platformExecutorService = Executors.newFixedThreadPool(platformThreadsPermits);

        virtualThreadsPermits = calculateSafeValueWithoutXPercent(permits, 50);
        semaphore = new Semaphore(virtualThreadsPermits);
    }


    public void startConversion() {
        virtualTaskList = new ArrayList<>();
        for (ConversionItemDTO conversionItemDTO : conversionItemDTOList) {
            conversionItemDTO.setStatus(ConversionStatus.QUEUED);
            ConvertSingleItemTask task = new ConvertSingleItemTask(conversionItemDTO, publishItemUpdate, this::setItemAsCompletedOverride, semaphore, platformExecutorService);
            virtualTaskList.add(task);
            virtualThreadExecutor.submit(task);
        }
        onCompleteAll();
    }

    private void setItemAsCompletedOverride(ConversionItemDTO conversionItemDTO) {
        updatedConversionItemDTOList.add(conversionItemDTO);
        setItemAsCompleted.accept(conversionItemDTO);
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

        } catch (InterruptedException e) {
            log.warn("Conversion Worker interrupted (user STOP action). Forcing Orchestrator cancel.", e);
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
            onAllTasksComplete();
        }
    }

    public void onAllTasksComplete() {
        onAllTasksComplete.accept(updatedConversionItemDTOList);
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
