package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.enums.NotificationLevel;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Builder
public record ConvertImageInputDTO(
        Path sourceFile,
        Path tmpPath,
        Path destinationPath,
        String targetExtension,
        Runnable onStart,
        Consumer<Float> onProgress,
        Consumer<ConversionItemDTO> pass,
        BiConsumer<ConversionItemDTO, Exception> fail,
        Consumer<ConversionItemDTO> addToFileQueue,
        NotificationLevel notificationLevel,
        BiConsumer<Path, ConvertImageInputDTO> renameMe,
        Consumer<ConversionItemDTO> copyMe,
        Function<ConversionItemDTO, Path> calculateTemporaryFilenameForMe,
        List<String> readerFormatNames
) {
}
