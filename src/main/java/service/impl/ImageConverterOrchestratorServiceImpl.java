package service.impl;

import dto.ImageConversionResultDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.ImageConverterOrchestratorService;
import service.ImageConverterService;
import util.CustomThreadFactory;

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
    public List<ImageConversionResultDTO> convertMultithreaded(List<String> imageFilesList, String destinationPath, String targetExtension) {
        Objects.requireNonNull(imageFilesList);

        try (ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new CustomThreadFactory())) {

            List<Future<ImageConversionResultDTO>> futureList = imageFilesList.stream()
                    .map(imageFile -> executorService.submit(getSinglegetCallable(destinationPath, imageFile, targetExtension)))
                    .toList();

            return futureList.stream()
                    .map(getFutureResultFunction())
                    .toList();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
            } catch (ExecutionException | InterruptedException e) {
                log.error(e.getMessage(), e);
                return ImageConversionResultDTO.builder()
                        .filename("fatal error")
                        .status(false)
                        .build();
            }
        };
    }

}
