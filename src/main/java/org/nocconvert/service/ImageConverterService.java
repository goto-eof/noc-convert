package org.nocconvert.service;

import org.nocconvert.dto.ImageConversionResultDTO;

import java.io.IOException;

public interface ImageConverterService {
    ImageConversionResultDTO convertImage(String sourceFileString, String destinationPath, String targetExtension) throws IOException;
}
