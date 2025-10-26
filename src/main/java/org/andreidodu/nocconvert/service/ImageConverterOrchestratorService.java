package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;

public interface ImageConverterOrchestratorService {
    List<ImageConversionResultDTO> convertMultithreaded(ExecutorService executorService, List<Path> imageFilesList, Path destinationPath, String targetExtension);
}
