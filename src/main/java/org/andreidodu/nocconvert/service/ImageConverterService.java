package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface ImageConverterService {
    void convertImage(Path sourceFile, Path destinationPath, String targetExtension, Consumer<Float> onProgress, Runnable onStart, Runnable onComplete) throws IOException;

    List<FormatExtensionDTO> getAvailableWriteFormatList();

    List<FormatExtensionDTO> getAvailableReadFormatList();
}
