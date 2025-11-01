package org.andreidodu.nocconvert.dto.conversion.input;

import lombok.Builder;

import java.nio.file.Path;
import java.util.function.Consumer;

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
        Consumer<Path> addToFileRenameQueue
) {
}
