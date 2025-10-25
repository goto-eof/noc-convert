package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;

import java.io.IOException;
import java.util.List;

public interface ImageConverterService {
    ImageConversionResultDTO convertImage(String sourceFileString, String destinationPath, String targetExtension) throws IOException;

    List<FormatExtensionDTO> getAvailableFormatExtensionList();
}
