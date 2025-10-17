package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;

import java.io.IOException;

public interface ImageConverterService {
    ImageConversionResultDTO convertImage(String sourceFileString, String destinationPath, String targetExtension) throws IOException;
}
