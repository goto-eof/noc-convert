package service;

import dto.ImageConversionResultDTO;

import java.util.List;

public interface ImageConverterFacadeService {
    List<String> getAllFilesDirectoryByExtension(String sourcePath, List<String> strings);

    List<ImageConversionResultDTO> convertFileListToCustomFormatMultithreaded(List<String> allFiles, String destinationPath, String imageFormat);
}
