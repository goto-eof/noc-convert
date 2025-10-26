package org.andreidodu.nocconvert.gui.controller.worker;

import lombok.Builder;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.FileSystemServiceImpl;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class ImageSearcherSwingWorker extends SwingWorker<List<File>, ImageSearcherSwingWorker.ChunkDTO> {
    private static final Logger log = LogManager.getLogger(ImageSearcherSwingWorker.class);

    private final Path sourceDirectory;
    private final FileSystemService fileSystemService;
    private final ImageConverterService imageConverterService;
    private final FormatExtensionMapper formatExtensionMapper;

    public ImageSearcherSwingWorker(Path sourceDirectory) {
        super();
        this.sourceDirectory = sourceDirectory;
        this.fileSystemService = new FileSystemServiceImpl();
        this.imageConverterService = new ImageConverterServiceImpl();
        this.formatExtensionMapper = new FormatExtensionMapper();
    }

    @Override
    protected List<File> doInBackground() throws Exception {
        fileSystemService.getAllFiles(sourceDirectory, getAvailableReadExtensions(), this::onDirectoryProcessed);

        return List.of();
    }

    private List<String> getAvailableReadExtensions() {
        return imageConverterService.getAvailableReadFormatList()
                .stream()
                .map(format -> FormatExtensionMapper.getExtension(format.getFormat()))
                .toList();
    }
    @Override
    protected void process(List<ChunkDTO> chunks) {

    }

    private void onDirectoryProcessed(Path path, int numFiles) {
        publish(ChunkDTO.builder().directory(path).numFiles(numFiles).build());
    }


    @Builder
    protected record ChunkDTO(
            Path directory,
            int numFiles) {
    }
}
