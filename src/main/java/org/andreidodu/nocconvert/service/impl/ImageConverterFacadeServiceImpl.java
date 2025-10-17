package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.andreidodu.nocconvert.service.ImageConverterFacadeService;
import org.andreidodu.nocconvert.service.ImageConverterOrchestratorService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ImageConverterFacadeServiceImpl implements ImageConverterFacadeService {
    private final FileSystemService fileSystemService;
    private final ImageConverterOrchestratorService imageConverterOrchestratorService;

    public ImageConverterFacadeServiceImpl() {
        this.fileSystemService = new FileSystemServiceImpl();
        this.imageConverterOrchestratorService = new ImageConverterOrchestratorServiceImpl();
    }

    @Override
    public List<String> getAllFilesDirectoryByExtension(String sourcePath, List<String> strings) {
        try {
            return fileSystemService.getAllFiles(sourcePath, strings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ImageConversionResultDTO> convertFileListToCustomFormatMultithreaded(ExecutorService executorService, List<String> allFiles, String destinationPath, String imageFormat) {
        createDestinationPath(destinationPath);
        return imageConverterOrchestratorService.convertMultithreaded(executorService, allFiles, destinationPath, imageFormat);
    }

    private static void createDestinationPath(String destinationPath) {
        try {
            Files.createDirectories(Path.of(destinationPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
