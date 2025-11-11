package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;
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
        Runnable pass,
        Consumer<Exception> fail,
        Consumer<Path> addToFileQueue,
        NotificationLevel notificationLevel,
        BiConsumer<Path, ConvertImageInputDTO> renameMe,
        Consumer<ConvertImageInputDTO> copyMe,
        Function<ConvertImageInputDTO, Path> calculateTemporaryFilenameForMe,
        List<String> readerFormatNames
) {
}
