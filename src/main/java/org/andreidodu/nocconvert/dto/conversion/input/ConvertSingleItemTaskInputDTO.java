package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

@Builder
public record ConvertSingleItemTaskInputDTO(
        ConversionItemDTO conversionItemDTO,
        Path tmpPath,
        Consumer<ConversionItemDTO> publishItemUpdate,
        Consumer<ConversionItemDTO> setItemAsCompleted,
        Semaphore semaphore,
        ExecutorService platformExecutorService,
        Runnable incrementPasses,
        Runnable incrementFailures,
        Consumer<Path> addToFileRenameQueue
) {
}
