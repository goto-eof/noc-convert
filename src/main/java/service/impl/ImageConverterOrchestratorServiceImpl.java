package service.impl;

import dto.ImageConversionResultDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.ImageConverterOrchestratorService;
import service.ImageConverterService;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

public class ImageConverterOrchestratorServiceImpl implements ImageConverterOrchestratorService {
    private static final Logger log = LogManager.getLogger(ImageConverterOrchestratorServiceImpl.class);

    private final ImageConverterService imageConverterService;

    public ImageConverterOrchestratorServiceImpl() {
        this.imageConverterService = new ImageConverterServiceImpl();
    }


    @Override
    public List<ImageConversionResultDTO> convertMultithreaded(ExecutorService executorService, List<String> imageFilesList, String destinationPath, String targetExtension) {
        Objects.requireNonNull(imageFilesList);

        try {
            List<Future<ImageConversionResultDTO>> futureList = imageFilesList.stream()
                    .map(imageFile -> executorService.submit(getSinglegetCallable(destinationPath, imageFile, targetExtension)))
                    .toList();

            return futureList.stream()
                    .map(getFutureResultFunction())
                    .toList();
        } catch (RejectedExecutionException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private Callable<ImageConversionResultDTO> getSinglegetCallable(String destinationPath, String imageFile, String targetExtension) {
        return () -> {
            try {
                return imageConverterService.convertImage(imageFile, destinationPath, targetExtension);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return ImageConversionResultDTO.builder().filename(e.getMessage()).status(false).build();
            }
        };
    }

    private static Function<Future<ImageConversionResultDTO>, ImageConversionResultDTO> getFutureResultFunction() {

        return future -> {
            try {
                return future.get();
            } catch (ExecutionException e) {
                log.error(e.getMessage(), e);
                return ImageConversionResultDTO.builder()
                        .filename("fatal error")
                        .status(false)
                        .build();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
                return ImageConversionResultDTO.builder()
                        .filename("error: user interruption")
                        .status(false)
                        .build();
            }
        };
    }

}
