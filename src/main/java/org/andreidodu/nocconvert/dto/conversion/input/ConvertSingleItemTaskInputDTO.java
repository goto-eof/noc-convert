package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.enums.NotificationLevel;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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
        Consumer<Path> addToFileRenameQueue,
        Consumer<Path> addToFileQueue,
        Supplier<Boolean> isParentInterrupted,
        NotificationLevel notificationLevel,
        BiConsumer<Path, ConvertImageInputDTO> renameMe,
        Consumer<ConvertImageInputDTO> copyMe,
        Function<ConvertImageInputDTO, Path> calculateTemporaryFilenameForMe,
        List<String> readerFormatNames
) {
}
