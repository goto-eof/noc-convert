package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public interface FileSystemService {
    List<Path> getAllFilesInDirectory(Path directoryPath,
                                      List<String> allowedFileExtensionList,
                                      FilesInDirectoryListener listener,
                                      Supplier<Boolean> isCancelled) throws IOException;

    boolean containsAtLeaseOneFile(Path directoryPath);
}
