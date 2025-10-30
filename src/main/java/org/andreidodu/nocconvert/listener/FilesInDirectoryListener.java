package org.andreidodu.nocconvert.listener;

import java.nio.file.Path;
import java.util.List;

public interface FilesInDirectoryListener {
    void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory);

    void onFileFound(Path filename);

    void onUpdateTotalFile(Long numberOfDirectories);

    void onSearchDone(List<Path> result);

    void onOperationAborted();
}
