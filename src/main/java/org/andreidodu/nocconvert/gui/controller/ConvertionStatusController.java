package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.components.ListItemRenderer;
import org.andreidodu.nocconvert.gui.controller.worker.ListRendererWorker;
import org.andreidodu.nocconvert.gui.dto.ConvertionStatusDTO;
import org.andreidodu.nocconvert.util.FileUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class ConvertionStatusController {
    private static final Logger log = LogManager.getLogger(ConvertionStatusController.class);
    private final ConvertionStatusDTO convertionStatusDTO;

    public ConvertionStatusController(ConvertionStatusDTO convertionStatusDTO) {
        this.convertionStatusDTO = convertionStatusDTO;
    }

    public void onSearchStepFinish(String targetFormat, List<Path> paths) {
        convertionStatusDTO.conversionFileListScrollPane().setVisible(true);
        SwingUtilities.invokeLater(() -> {
            long startTime = System.nanoTime();

            DefaultListModel<ConversionItemDTO> modelProgress = new DefaultListModel<>();
            convertionStatusDTO.conversionFileList().setModel(modelProgress);
            ListItemRenderer renderer = new ListItemRenderer();
            convertionStatusDTO.conversionFileList().setCellRenderer(renderer);

            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;
            double durationMs = durationNs / 1_000_000.0;

            log.debug("rendering time for 0 records: {} ms", durationMs);
        });
        List<ConversionItemDTO> list = convertPathListToDTOList(targetFormat, paths);
        ListRendererWorker listRendererWorker = new ListRendererWorker(
                list,
                convertionStatusDTO.conversionFileList(),
                () -> convertionStatusDTO.guiOrchestrator().onRenderingDone(list)
        );
        listRendererWorker.execute();
    }

    private static List<ConversionItemDTO> convertPathListToDTOList(String targetFormat, List<Path> paths) {
        return paths.stream().map(path -> ConversionItemDTO.builder()
                .targetFormat(targetFormat)
                .path(path)
                .fileName(prepareFileName(path))
                .status(ConversionStatus.QUEUED)
                .progressPercentage(0)
                .fileSize(FileUtil.getHumanableFileSize(path.toFile()))
                .build()).toList();
    }

    private static String prepareFileName(Path path) {
        String filename = path.getFileName().toString();
        int maxLength = 50;
        if (filename.length() > maxLength) {
            filename = filename.substring(0, maxLength) + "...";
        }
        return filename;
    }
}
