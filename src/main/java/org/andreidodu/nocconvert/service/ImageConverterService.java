package org.andreidodu.nocconvert.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface ImageConverterService {

    void convertImage(Path sourceFile, Path destinationPath, String targetExtension, Runnable onStart, Consumer<Float> onProgress) throws IOException;

    void cancelTask(boolean interrupt);

    void interruptTask();
}
