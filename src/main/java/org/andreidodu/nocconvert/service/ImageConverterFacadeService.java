package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;

public interface ImageConverterFacadeService {
    List<Path> getAllFilesDirectoryByExtension(String sourcePath, List<String> strings);

    List<ImageConversionResultDTO> convertFileListToCustomFormatMultithreaded(ExecutorService executorService, List<Path> allFiles, Path destinationPath, String imageFormat);
}
