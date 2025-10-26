package org.andreidodu.nocconvert.service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface ImageConverterOrchestratorService {
    void convertMultithreaded(ExecutorService executorService, List<Path> imageFilesList, Path destinationPath, String targetExtension, Consumer<Float> onProgress, Runnable onStart, Runnable onComplete);
}
