package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import java.util.List;
import java.util.function.Consumer;

@Builder
public record ConversionOrchestratorInputDTO(
        List<ConversionItemDTO> conversionItemDTOList,
        Consumer<ConversionItemDTO> publishItemUpdate,
        Consumer<ConversionItemDTO> setItemAsCompleted,
        Runnable onAllTasksCompleteEnableComponents,
        Runnable incrementPasses,
        Runnable incrementFailures
) {
}
