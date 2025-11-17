package org.andreidodu.nocconvert.gui.worker.searcher;

import lombok.Builder;
import lombok.Getter;
import org.andreidodu.nocconvert.exception.ManualAbortedException;
import org.andreidodu.nocconvert.helper.EDTLogger;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.task.PictureSearcherTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ImageSearcherSwingWorker extends SwingWorker<Collection<Path>, ImageSearcherSwingWorker.ChunkDTO> {
    private static final Logger log = LogManager.getLogger(ImageSearcherSwingWorker.class);


    private final Path sourceDirectory;
    private final FilesInDirectoryListener filesInDirectoryListener;
    private final PictureSearcherTask pictureSearcherTask;

    private List<Path> validSearchResult;
    private STATUS status = STATUS.SEARCHING;
    private final Consumer<List<Path>> onSearchComplete;

    private final int UI_UPDATE_INTERVAL_FILE = 200;
    private final int uiUpdateIntervalFileCounter = 0;
    private final int UI_UPDATE_INTERVAL_DIRECTORY = 200;
    private final int uiUpdateIntervalDirectoryCounter = 0;
    private final static long GUI_UPDATE_INTERVAL = 50;

    private LocalDateTime lastFileInfoPrintTS = LocalDateTime.now();
    private final LocalDateTime lastDirectoryInfoPrintTS = LocalDateTime.now();
    private final LocalDateTime lastTotalInfoPrintTS = LocalDateTime.now();

    enum STATUS {
        SEARCHING, ANALYZING
    }

    public ImageSearcherSwingWorker(Path sourceDirectory, FilesInDirectoryListener filesInDirectoryListener, Consumer<List<Path>> onSearchComplete) {
        super();
        this.sourceDirectory = sourceDirectory;
        this.filesInDirectoryListener = filesInDirectoryListener;
        this.onSearchComplete = onSearchComplete;
        pictureSearcherTask = new PictureSearcherTask();
    }

    @Override
    protected Collection<Path> doInBackground() {
        log.debug("Starting to search image from directory {}", sourceDirectory);
        FilesInDirectoryListenerImpl listener = new FilesInDirectoryListenerImpl();
        try {
            validSearchResult = pictureSearcherTask.search(sourceDirectory, listener).stream().map(File::toPath).toList();
            log.debug("Finished searching image from directory {}", sourceDirectory);
            if (listener.getError() != null) {
                throw listener.getError();
            }
            // validationPictureTask = new ValidationPictureTask(rawSearchResult, listener, super::isCancelled);
            // validSearchResult = validationPictureTask.validateAndGetSearchResult();
            return validSearchResult;
        } catch (AccessDeniedException e) {
            listener.onAccessDenied();
            log.error("Access denied for: {}", sourceDirectory, e);
        } catch (Exception e) {
            if (e instanceof ManualAbortedException) {
                listener.onOperationAborted();
                filesInDirectoryListener.onOperationAborted();
                return new ArrayList<>();
            }
            log.error(getRootCauseMessage(e), e);
            onSearchComplete.accept(new ArrayList<>());
            listener.onUpdateTotalFile(0L);
        }
        return new ArrayList<>();
    }

    @Override
    protected void done() {
        onSearchComplete.accept(validSearchResult);
    }

    public Void shutdown() {
        Optional.ofNullable(pictureSearcherTask).ifPresent(PictureSearcherTask::cancel);
        //Optional.ofNullable(validationPictureTask).ifPresent(ValidationPictureTask::cancel);
        this.cancel(true); // worker
        return null;
    }

    @Builder
    protected record ChunkDTO(Path directory, long numFiles) {

    }

    @Override
    protected void process(List<ChunkDTO> chunks) {
        if (isCancelled() || pictureSearcherTask == null || pictureSearcherTask.isCancelled()) {
            return;
        }
        EDTLogger.logEDTActivity("Why?", () -> {
            Path directory = chunks.getLast().directory;
            if (status == STATUS.SEARCHING) {
                filesInDirectoryListener.onFileFound(0, directory);
            } else if (status == STATUS.ANALYZING) {
                filesInDirectoryListener.onFileAnalyzationComplete(directory);
            }
            filesInDirectoryListener.onFileAnalyzationProgress(chunks.size());


        });
    }

    public class FilesInDirectoryListenerImpl implements FilesInDirectoryListener {
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
            if (Duration.between(lastFileInfoPrintTS, LocalDateTime.now()).toMillis() < GUI_UPDATE_INTERVAL || isCancelled() || pictureSearcherTask == null || pictureSearcherTask.isCancelled()) {
                return;
            }
            publish(ChunkDTO.builder().directory(filename).build());
            lastFileInfoPrintTS = LocalDateTime.now();
        }

        @Override
        public void onFileAnalyzationStart(int totalNumberFiles) {
            status = STATUS.ANALYZING;
            filesInDirectoryListener.onFileAnalyzationStart(totalNumberFiles);
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
            if (Duration.between(lastFileInfoPrintTS, LocalDateTime.now()).toMillis() < GUI_UPDATE_INTERVAL || isCancelled() || pictureSearcherTask == null || pictureSearcherTask.isCancelled()) {
                return;
            }
            publish(ChunkDTO.builder().directory(filename).build());
            lastFileInfoPrintTS = LocalDateTime.now();
        }

        @Override
        public void onUpdateTotalFile(Long numberOfDirectories) {
            filesInDirectoryListener.onUpdateTotalFile(numberOfDirectories);
        }

        @Override
        public void onOperationAborted() {
        }

        @Override
        public void onAccessDenied() {
            filesInDirectoryListener.onAccessDenied();
        }
    }

}
