package service.impl;

import dto.ImageConversionResultDTO;
import service.FileSystemService;
import service.ImageConverterFacadeService;
import service.ImageConverterOrchestratorService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    public List<ImageConversionResultDTO> convertFileListToCustomFormatMultithreaded(List<String> allFiles, String destinationPath, String imageFormat) {
        createDestinationPath(destinationPath);
        return imageConverterOrchestratorService.convertMultithreaded(allFiles, destinationPath, imageFormat);
    }

    private static void createDestinationPath(String destinationPath) {
        try {
            Files.createDirectories(Path.of(destinationPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
