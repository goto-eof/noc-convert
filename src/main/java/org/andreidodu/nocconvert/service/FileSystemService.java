package org.andreidodu.nocconvert.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

public interface FileSystemService {
    List<Path> getAllFiles(Path directoryPath, List<String> allowedFileExtensionList, BiConsumer<Path, Integer> onDirectoryProcessed) throws IOException;

    boolean containsAtLeaseOneFile(Path directoryPath);
}
