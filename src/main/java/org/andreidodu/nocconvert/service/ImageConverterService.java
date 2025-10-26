package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ImageConverterService {
    ImageConversionResultDTO convertImage(Path sourceFile, Path destinationPath, String targetExtension) throws IOException;

    List<FormatExtensionDTO> getAvailableWriteFormatList();

    List<FormatExtensionDTO> getAvailableReadFormatList();
}
