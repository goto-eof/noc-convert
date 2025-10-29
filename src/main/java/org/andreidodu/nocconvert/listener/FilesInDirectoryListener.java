package org.andreidodu.nocconvert.listener;

import java.nio.file.Path;
import java.util.List;

public interface FilesInDirectoryListener {
    void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory);

    void onFileFound(Path path);

    void onUpdateTotalFile(Long numberOfDirectories);

    void onDone(List<Path> result);

    void onOperationAborted();
}
