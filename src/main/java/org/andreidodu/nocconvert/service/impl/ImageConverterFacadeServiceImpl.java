package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.andreidodu.nocconvert.service.ImageConverterFacadeService;
import org.andreidodu.nocconvert.service.ImageConverterOrchestratorService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    public List<Path> getAllFilesDirectoryByExtension(String sourcePath, List<String> strings) {
        if (!new File(sourcePath).exists()) {
            return new ArrayList<>();
        }
        try {
            return fileSystemService.getAllFiles(Path.of(sourcePath), strings, (a, b) -> {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ImageConversionResultDTO> convertFileListToCustomFormatMultithreaded(ExecutorService executorService, List<Path> allFiles, Path destinationPath, String imageFormat) {
        createDestinationPath(destinationPath);
        return imageConverterOrchestratorService.convertMultithreaded(executorService, allFiles, destinationPath, imageFormat);
    }

    private static void createDestinationPath(Path destinationPath) {
        try {
            Files.createDirectories(destinationPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
