package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Builder
public record SearchAndConvertInputDTO(
        List<ConversionItemDTO> list,
        Consumer<List<ConversionItemDTO>> updateGui,
        Runnable onAllTasksComplete,
        Consumer<ConversionItemDTO> completeItem,
        Runnable incrementPasses,
        Runnable decrementPasses,
        Runnable incrementFailures,
        Runnable onConversionAborted,
        Consumer<Path> addToDeletionQueue,
        Path sourceDirectory,
        Path destinationDirectory,
        String targetFormat,
        Consumer<List<ConversionItemDTO>> endSearchChunkForImagesStep,
        Consumer<Integer> incrementMainProgressBarProgress,
        Consumer<Integer> updateSecondaryProgressBarMaxValue,
        Runnable onBucketItemsCompleted,
        Runnable resetSecondaryProgressBar,
        Runnable updateSecondaryProgressBarColorColorToBlue,
        BiConsumer<Integer, Path> onFileFound,
        Consumer<Boolean> showJListPane
) {
}
