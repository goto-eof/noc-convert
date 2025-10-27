package org.andreidodu.nocconvert.gui.worker;

import lombok.Builder;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.FileSystemServiceImpl;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class ImageSearcherSwingWorker extends SwingWorker<List<Path>, ImageSearcherSwingWorker.ChunkDTO> {
    private static final Logger log = LogManager.getLogger(ImageSearcherSwingWorker.class);

    private final Path sourceDirectory;
    private final FileSystemService fileSystemService;
    private final ImageConverterService imageConverterService;
    private final Consumer<String> updateApplicationStatusLabel;
    private final Consumer<List<Path>> onSearchDone;

    public ImageSearcherSwingWorker(Path sourceDirectory, Consumer<String> updateApplicationStatusLabel, Consumer<List<Path>> onSearchDone) {
        super();
        this.sourceDirectory = sourceDirectory;
        this.updateApplicationStatusLabel = updateApplicationStatusLabel;
        this.onSearchDone = onSearchDone;

        this.fileSystemService = new FileSystemServiceImpl();
        this.imageConverterService = new ImageConverterServiceImpl();
    }

    @Override
    protected List<Path> doInBackground() throws Exception {
        log.debug("Starting to search image from directory {}", sourceDirectory);
        return fileSystemService.getAllFiles(sourceDirectory, getAvailableReadExtensions(), this::onDirectoryProcessed);
    }

    private List<String> getAvailableReadExtensions() {
        return imageConverterService.getAvailableReadFormatList()
                .stream()
                .map(format -> FormatExtensionMapper.getExtension(format.getFormat()))
                .toList();
    }

    @Override
    protected void process(List<ChunkDTO> chunks) {
        chunks.forEach(chunk -> {
            String message = String.format("Searching for images: the directory %s contains %s processable images", chunk.directory.getFileName().toString(), chunk.numFiles);
            updateApplicationStatusLabel.accept(message);
        });
    }

    private void onDirectoryProcessed(Path path, int numFiles) {
        publish(ChunkDTO.builder().directory(path).numFiles(numFiles).build());
    }

    @Override
    protected void done() {
        try {
            List<Path> processingJobResult = get();
            onSearchDone.accept(processingJobResult);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Builder
    protected record ChunkDTO(
            Path directory,
            int numFiles) {
    }
}
