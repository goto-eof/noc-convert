package org.andreidodu.nocconvert.gui.worker;

import lombok.Builder;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.task.PictureSearcher;
import org.andreidodu.nocconvert.util.ImageConverterUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ImageSearcherSwingWorker extends SwingWorker<Collection<File>, ImageSearcherSwingWorker.ChunkDTO> {
    private static final Logger log = LogManager.getLogger(ImageSearcherSwingWorker.class);

    private final Path sourceDirectory;
    private final ImageConverterUtil imageConverterService;
    private final FilesInDirectoryListener filesInDirectoryListener;
    private final PictureSearcher pictureSearcher;

    public ImageSearcherSwingWorker(Path sourceDirectory, FilesInDirectoryListener filesInDirectoryListener) {
        super();
        this.sourceDirectory = sourceDirectory;
        this.filesInDirectoryListener = filesInDirectoryListener;
        this.pictureSearcher = new PictureSearcher();

        this.imageConverterService = new ImageConverterUtil();
    }

    @Override
    protected Collection<File> doInBackground() {
        log.debug("Starting to search image from directory {}", sourceDirectory);
        FilesInDirectoryListenerImpl listener = new FilesInDirectoryListenerImpl();
        try {
            listener.onSearchDone(pictureSearcher.search(sourceDirectory, listener, getAvailableReadExtensions()).stream().map(File::toPath).toList());
            log.debug("Finished searching image from directory {}", sourceDirectory);
        } catch (AccessDeniedException e) {
            listener.onAccessDenied();
            log.error("Access denied for: {}", sourceDirectory, e);
        } catch (Exception e) {
            log.error(getRootCauseMessage(e), e);
            listener.onUpdateTotalFile(0L);
            listener.onSearchDone(new ArrayList<>());
        }
        return new ArrayList<>();
    }


    private List<String> getAvailableReadExtensions() {
        List<String> list = new ArrayList<>(imageConverterService.getAvailableReadFormatList()
                .stream()
                .map(format -> FormatExtensionMapper.getExtension(format.getFormat().toLowerCase()))
                .toList());
        if (list.stream().anyMatch(item -> item.equalsIgnoreCase("jpg"))) {
            list.add(FormatExtensionMapper.getExtension("jpeg"));
        }

        list = list.stream().map(String::toLowerCase).distinct().toList();

        return list;
    }

    @Override
    protected void done() {
        // NOTE: we're already passing the result through the FilesInDirectoryListenerImpl
    }

    public Void shutdown() {
        this.cancel(true);
        pictureSearcher.cancel();
        return null;
    }

    @Builder
    protected record ChunkDTO(
            Path directory,
            long numFiles) {
    }

    private class FilesInDirectoryListenerImpl implements FilesInDirectoryListener {
        @Override
        public void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory) {
            publish(ChunkDTO.builder().directory(directory).numFiles(filesInDirectory).build());
            filesInDirectoryListener.onDirectoryProcessed(totalFiles, directory, filesInDirectory);
        }

        @Override
        public void onFileFound(Path filename) {
            filesInDirectoryListener.onFileFound(filename);
        }

        @Override
        public void onUpdateTotalFile(Long numberOfDirectories) {
            filesInDirectoryListener.onUpdateTotalFile(numberOfDirectories);
        }

        @Override
        public void onSearchDone(List<Path> result) {
            filesInDirectoryListener.onSearchDone(result);
        }

        @Override
        public void onOperationAborted() {
            filesInDirectoryListener.onOperationAborted();
        }

        @Override
        public void onAccessDenied() {
            filesInDirectoryListener.onAccessDenied();
        }
    }

}
