package service;

import dto.ImageConversionResultDTO;

import java.util.List;
import java.util.concurrent.ExecutorService;

public interface ImageConverterOrchestratorService {
    List<ImageConversionResultDTO> convertMultithreaded(ExecutorService executorService, List<String> imageFilesList, String destinationPath, String targetExtension);
}
