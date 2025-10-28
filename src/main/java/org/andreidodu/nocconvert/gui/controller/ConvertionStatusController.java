package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.components.ListItemRenderer;
import org.andreidodu.nocconvert.gui.constants.Colors;
import org.andreidodu.nocconvert.gui.dto.ConvertionStatusDTO;
import org.andreidodu.nocconvert.gui.worker.ListRendererWorker;
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

        convertionStatusDTO.progressBar().setStringPainted(true);
    }

    public void onSearchStepFinish(Path destinationDirectory, String targetFormat, List<Path> paths) {


        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.conversionFileListScrollPane().setVisible(true);
            convertionStatusDTO.scrollPanePanel().setVisible(true);

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

        List<ConversionItemDTO> list = convertPathListToDTOList(destinationDirectory, targetFormat, paths);
        ListRendererWorker listRendererWorker = new ListRendererWorker(list, convertionStatusDTO.conversionFileList(), () -> convertionStatusDTO.guiOrchestrator().onRenderingDone(list));
        listRendererWorker.execute();
    }

    private static List<ConversionItemDTO> convertPathListToDTOList(Path destPath, String targetFormat, List<Path> paths) {
        return paths.stream()
                .map(path -> ConversionItemDTO.builder()
                        .targetFormat(targetFormat)
                        .targetExtension(targetFormat)
                        .sourceFile(path)
                        .fileName(prepareFileName(path))
                        .status(ConversionStatus.QUEUED)
                        .progressPercentage(0f)
                        .sourceFile(path)
                        .destinationDirectory(destPath)
                        .fileSize(FileUtil.getHumanableFileSize(path.toFile()))
                        .build())
                .toList();
    }

    private static String prepareFileName(Path path) {
        String filename = path.getFileName().toString();
        int maxLength = 50;
        if (filename.length() > maxLength) {
            filename = filename.substring(0, maxLength) + "...";
        }
        return filename;
    }

    public void updateList(List<ConversionItemDTO> list) {
        SwingUtilities.invokeLater(() -> {
            DefaultListModel<ConversionItemDTO> model = (DefaultListModel<ConversionItemDTO>) convertionStatusDTO.conversionFileList().getModel();
            for (ConversionItemDTO conversionItemDTO : list) {
                model.set(conversionItemDTO.getIndex(), conversionItemDTO);
            }
            convertionStatusDTO.conversionFileList().repaint();
        });
    }

    public void enableProgressBar() {
        convertionStatusDTO.conversionFileList().setModel(new DefaultListModel<>());
        convertionStatusDTO.conversionFileListScrollPane().setVisible(false);
        convertionStatusDTO.progressBarAndStatusPanel().setVisible(true);
        convertionStatusDTO.progressBar().setVisible(true);
    }

    public void updateMainProgressBarMaxValue(int size) {
        convertionStatusDTO.progressBar().setMaximum(size);
        convertionStatusDTO.progressBar().setStringPainted(true);
    }

    public void conversionDone() {
        onAllTaskComplete();
    }

    public void updateMainProgressBarProgress(int completedItems) {
        convertionStatusDTO.progressBar().setValue(completedItems);
    }

    public void incrementMainProgressBarProgress() {
        convertionStatusDTO.progressBar().setValue(convertionStatusDTO.progressBar().getValue() + 1);
    }

    public void startSearch() {
        convertionStatusDTO.scrollPanePanel().setVisible(false);
    }

    public void noFileFound() {
        convertionStatusDTO.conversionFileListScrollPane().setVisible(false);
        convertionStatusDTO.progressBarAndStatusPanel().setVisible(false);
        convertionStatusDTO.progressBar().setVisible(false);
    }

    public void onAllTaskComplete() {
        convertionStatusDTO.progressBar().setForeground(Colors.LIME_DARK);
        convertionStatusDTO.progressBar().setValue(convertionStatusDTO.progressBar().getMaximum());

    }

    public void onConversionStart() {
        convertionStatusDTO.progressBar().setForeground(Colors.ACCENT_BLUE);
    }
}
