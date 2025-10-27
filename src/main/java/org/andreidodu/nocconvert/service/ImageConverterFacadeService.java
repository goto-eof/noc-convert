package org.andreidodu.nocconvert.service;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public interface ImageConverterFacadeService {
    List<Path> getAllFilesDirectoryByExtension(String sourcePath, List<String> strings);

  void convertFileListToCustomFormatMultithreaded(ExecutorService executorService, List<Path> allFiles, Path destinationPath, String imageFormat, Consumer<Float> onProgress, Runnable onStart, Runnable onComplete);
}
