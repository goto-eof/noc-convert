package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import java.util.List;
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
        Runnable onConversionAborted
) {
}
