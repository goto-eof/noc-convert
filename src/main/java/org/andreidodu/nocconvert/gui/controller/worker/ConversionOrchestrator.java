package org.andreidodu.nocconvert.gui.controller.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ConversionOrchestrator {
    private final Consumer<ConversionItemDTO> publish;
    private final Runnable onItemCompleted;

    public ConversionOrchestrator(
            List<ConversionItemDTO> conversionItemDTOList,
            Consumer<ConversionItemDTO> publish,
            Runnable onItemCompleted
    ) {

        this.publish = publish;
        this.onItemCompleted = onItemCompleted;

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            for (ConversionItemDTO conversionItemDTO : conversionItemDTOList) {
                executorService.submit(new ConvertSingleItemTask(conversionItemDTO, publish, onItemCompleted));
            }
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(Integer.MAX_VALUE, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

        }
    }
}
