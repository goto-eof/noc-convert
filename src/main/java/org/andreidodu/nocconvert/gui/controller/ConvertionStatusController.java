package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.components.ListItemRenderer;
import org.andreidodu.nocconvert.gui.dto.ConvertionStatusDTO;
import org.andreidodu.nocconvert.util.FileUtil;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class ConvertionStatusController {
    private final ConvertionStatusDTO convertionStatusDTO;

    public ConvertionStatusController(ConvertionStatusDTO convertionStatusDTO) {
        this.convertionStatusDTO = convertionStatusDTO;
    }

    public void onSearchStepFinish(String targetFormat, List<Path> paths) {
        SwingUtilities.invokeLater(() -> {
            long startTime = System.nanoTime();
            convertionStatusDTO.conversionFileListScrollPane().setVisible(true);
            DefaultListModel<ConversionItemDTO> modelProgress = new DefaultListModel<>();
            ListItemRenderer renderer = new ListItemRenderer();

            modelProgress.addAll(
                    paths.stream().map(path -> ConversionItemDTO.builder()
                                    .targetFormat(targetFormat)
                                    .fileName(prepareFileName(path))
                                    .status(ConversionStatus.QUEUED)
                                    .progressPercentage(0)
                                    .fileSize(FileUtil.getHumanableFileSize(path.toFile()))
                                    .build()
                            )
                            .toList()
            );

            convertionStatusDTO.conversionFileList().setModel(modelProgress);
            convertionStatusDTO.conversionFileList().setCellRenderer(renderer);
            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;
            double durationMs = durationNs / 1_000_000.0;

            System.out.println("Tempo di Blocco EDT per rendering 10k record: " + durationMs + " ms");
        });

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
