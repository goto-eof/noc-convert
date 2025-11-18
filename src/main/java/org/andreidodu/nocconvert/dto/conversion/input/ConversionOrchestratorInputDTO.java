package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

@Builder
public record ConversionOrchestratorInputDTO(
        List<ConversionItemDTO> conversionItemDTOList,
        Consumer<ConversionItemDTO> publishItemUpdate,
        Consumer<ConversionItemDTO> setItemAsCompleted,
        Runnable onAllTasksCompleteEnableComponents,
        Runnable incrementPasses,
        Runnable decrementSuccesses,
        Runnable incrementFailures,
        Runnable onConversionAborted,
        Consumer<Path> addToDeletionQueue,
        Semaphore mainSemaphore,
        CountDownLatch countDownLatchWorkFinished,
        int virtualThreadsPermits,
        ExecutorService virtualThreadsExecutor,
        ExecutorService platformThreadsExecutor,
        Path finalDirectory,
        Path temporaryDirectory,
        Consumer<Path> updateTmpDestinationDirectory
) {
}
