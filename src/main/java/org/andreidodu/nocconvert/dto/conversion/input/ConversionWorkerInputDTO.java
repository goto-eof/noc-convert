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
public record ConversionWorkerInputDTO(
        List<ConversionItemDTO> list,
        Consumer<List<ConversionItemDTO>> updateGui,
        Runnable onAllTasksComplete,
        Consumer<ConversionItemDTO> completeItem,
        Runnable incrementPasses,
        Runnable decrementPasses,
        Runnable incrementFailures,
        Runnable onConversionAborted,
        Consumer<Path> addToDeletionQueue,
        Semaphore semaphore,
        Consumer<List<Path>> endSearchChunkForImagesStep,
        CountDownLatch countDownLatchWorkFinished,
        ExecutorService virtualThreadsExecutor,
        int virtualThreadsPermits,
        int platformThreadsPermits,
        ExecutorService platformThreadsExecutor,
        Path finalDirectory,
        Path temporaryDirectory,
        Consumer<Path> updateTmpDestinationDirectory
) {
}
