package org.andreidodu.nocconvert.gui.worker.hybrid.listener;

import lombok.Getter;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;

import java.nio.file.Path;

public class FilesInDirectoryListenerImpl implements FilesInDirectoryListener {
    private final static int GUI_UPDATE_INTERVAL = 50;

    @Getter
    private Exception error;

    @Override
    public void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory) {
//            if (Duration.between(lastDirectoryInfoPrintTS, LocalDateTime.now()).toMillis() < 1000 || isCancelled() || pictureSearcherTask == null || pictureSearcherTask.isCancelled()) {
//                return;
//            }
//
//            publish(ChunkDTO.builder().directory(directory).numFiles(filesInDirectory).build());
////            filesInDirectoryListener.onDirectoryProcessed(totalFiles, directory, filesInDirectory);
//            lastDirectoryInfoPrintTS =  LocalDateTime.now();
    }

    @Override
    public void onFileFound(long totalFiles, Path filename) {
//        if (Duration.between(lastFileInfoPrintTS, LocalDateTime.now()).toMillis() < GUI_UPDATE_INTERVAL || isCancelled() || pictureSearcherTask == null || pictureSearcherTask.isCancelled()) {
//            return;
//        }
//        publish(ImageSearcherSwingWorker.ChunkDTO.builder().directory(filename).build());
//        lastFileInfoPrintTS = LocalDateTime.now();
    }

    @Override
    public void onFileAnalyzationStart(int totalNumberFiles) {
//        status = ImageSearcherSwingWorker.STATUS.ANALYZING;
//        filesInDirectoryListener.onFileAnalyzationStart(totalNumberFiles);
    }

    @Override
    public void onFileAnalyzationProgress(Integer totalNumberOfUpdates) {
//            if (Duration.between(lastFileInfoPrintTS, LocalDateTime.now()).toMillis() < 1000) {
//                return;
//            }
//
//            lastFileInfoPrintTS = LocalDateTime.now();
    }

    @Override
    public void onFileAnalyzationComplete(Path filename) {
//        if (Duration.between(lastFileInfoPrintTS, LocalDateTime.now()).toMillis() < GUI_UPDATE_INTERVAL || isCancelled() || pictureSearcherTask == null || pictureSearcherTask.isCancelled()) {
//            return;
//        }
//        publish(ImageSearcherSwingWorker.ChunkDTO.builder().directory(filename).build());
//        lastFileInfoPrintTS = LocalDateTime.now();
    }

    @Override
    public void onUpdateTotalFile(Long numberOfDirectories) {
//        filesInDirectoryListener.onUpdateTotalFile(numberOfDirectories);
    }

    @Override
    public void onOperationAborted() {
    }

    @Override
    public void onAccessDenied() {
//        filesInDirectoryListener.onAccessDenied();
    }
}