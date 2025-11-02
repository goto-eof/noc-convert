package org.andreidodu.nocconvert.gui.worker;

import lombok.Builder;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.andreidodu.nocconvert.service.impl.FileSystemServiceImpl;
import org.andreidodu.nocconvert.util.ImageConverterUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImageSearcherSwingWorker extends SwingWorker<List<Path>, ImageSearcherSwingWorker.ChunkDTO> {
    private static final Logger log = LogManager.getLogger(ImageSearcherSwingWorker.class);

    private final Path sourceDirectory;
    private final FileSystemService fileSystemService;
    private final ImageConverterUtil imageConverterService;
    private final FilesInDirectoryListener filesInDirectoryListener;

    public ImageSearcherSwingWorker(Path sourceDirectory, FilesInDirectoryListener filesInDirectoryListener) {
        super();
        this.sourceDirectory = sourceDirectory;
        this.filesInDirectoryListener = filesInDirectoryListener;

        this.fileSystemService = new FileSystemServiceImpl();
        this.imageConverterService = new ImageConverterUtil();
    }

    @Override
    protected List<Path> doInBackground() {
        log.debug("Starting to search image from directory {}", sourceDirectory);
        FilesInDirectoryListenerImpl listener = new FilesInDirectoryListenerImpl();
        try {
            return fileSystemService.getAllFilesInDirectory(sourceDirectory, getAvailableReadExtensions(), listener, super::isCancelled);
        } catch (AccessDeniedException e) {
            log.error(e.getMessage(), e);
            listener.onAccessDenied();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
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

    public void shutdown(boolean b) {
        this.cancel(b);
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
