package org.andreidodu.nocconvert.gui.worker;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.util.performance.AdaptiveGovernorRunnable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ConversionOrchestrator {
    private static final Logger log = LogManager.getLogger(ConversionOrchestrator.class);
    private final Consumer<ConversionItemDTO> publish;
    private final Consumer<ConversionItemDTO> onItemCompleted;
    @Getter
    private final ExecutorService executorService;
    private final List<ConversionItemDTO> conversionItemDTOList;
    private List<ConvertSingleItemTask> taskList;
    private static Semaphore semaphore;
    private final Runnable onAllTasksComplete;

    public ConversionOrchestrator(
            List<ConversionItemDTO> conversionItemDTOList,
            Consumer<ConversionItemDTO> publish,
            Consumer<ConversionItemDTO> onItemCompleted,
            Runnable onAllTasksComplete
    ) {

        this.publish = publish;
        this.onItemCompleted = onItemCompleted;
        this.onAllTasksComplete = onAllTasksComplete;
        this.conversionItemDTOList = conversionItemDTOList;
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        semaphore = new Semaphore(AdaptiveGovernorRunnable.VT_OPTIMAL_LIMIT);
    }

    public void startConversion() {
        taskList = new ArrayList<>();
        for (ConversionItemDTO conversionItemDTO : conversionItemDTOList) {
            ConvertSingleItemTask task = new ConvertSingleItemTask(conversionItemDTO, publish, onItemCompleted, semaphore);
            taskList.add(task);
            executorService.submit(task);
        }
        try {
            getExecutorService().shutdown();
            boolean terminated = getExecutorService().awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            if (!terminated) {
                log.error("Executor did not terminate in time. Forcing emergency shutdown.");
                getExecutorService().shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("Conversion Worker interrupted (user STOP action). Forcing Orchestrator cancel.", e);
            Thread.currentThread().interrupt();
            shutdown();
        } catch (Exception e) {
            shutdown();
        } finally {
            onAllTasksComplete.run();
        }
    }

    public void shutdown() {
        if (executorService != null && !executorService.isTerminated()) {
            taskList.forEach(ConvertSingleItemTask::closeStreams);
            this.executorService.shutdownNow();
        }
    }
}
