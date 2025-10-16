package service;

import dto.ImageConversionResultDTO;

import java.util.List;

public interface ImageConverterOrchestratorService {
    List<ImageConversionResultDTO> convertMultithreaded(List<String> imageFilesList, String destinationPath, String targetExtension);
}
