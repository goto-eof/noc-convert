package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.service.ImageConverterOrchestratorService;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

public class ImageConverterOrchestratorServiceImpl implements ImageConverterOrchestratorService {
    private static final Logger log = LogManager.getLogger(ImageConverterOrchestratorServiceImpl.class);

    private final ImageConverterService imageConverterService;

    public ImageConverterOrchestratorServiceImpl() {
        this.imageConverterService = new ImageConverterServiceImpl();
    }


    @Override
    public void convertMultithreaded(ExecutorService executorService, List<Path> imageFilesList, Path destinationPath, String targetExtension, Consumer<Float> onProgress, Runnable onStart, Runnable onComplete) {
        Objects.requireNonNull(imageFilesList);

        try {
            imageFilesList.forEach(imageFile -> executorService.submit(getSingleRunnable(
                    destinationPath,
                    imageFile,
                    targetExtension,
                    onProgress,
                    onStart,
                    onComplete
            )));
        } catch (RejectedExecutionException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private Runnable getSingleRunnable(Path destinationPath, Path imageFile, String targetExtension, Consumer<Float> onProgress, Runnable onStart, Runnable onComplete) {
        return () -> {
            try {
                imageConverterService.convertImage(
                        imageFile,
                        destinationPath,
                        targetExtension,
                        onStart,
                        onProgress,
                        onComplete
                );
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };
    }

}
