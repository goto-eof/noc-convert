package org.andreidodu.nocconvert.listener;

import java.nio.file.Path;

public interface FilesInDirectoryListener {
    void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory);

    void onFileFound(Path filename);

    void onFileAnalyzationStart(int totalNumberFiles);

    void onFileAnalyzationProgress(Integer totalNumberOfUpdates);

    void onFileAnalyzationComplete(Path filename);

    void onUpdateTotalFile(Long numberOfDirectories);

    void onOperationAborted();

    void onAccessDenied();
}
