package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface ImageConverterFacadeService {
    List<String> getAllFilesDirectoryByExtension(String sourcePath, List<String> strings);

    List<ImageConversionResultDTO> convertFileListToCustomFormatMultithreaded(ExecutorService executorService, List<String> allFiles, String destinationPath, String imageFormat);
}
