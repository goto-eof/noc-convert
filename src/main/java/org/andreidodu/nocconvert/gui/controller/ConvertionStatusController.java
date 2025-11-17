package org.andreidodu.nocconvert.gui.controller;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.gui.components.performant.PerformantListModel;
import org.andreidodu.nocconvert.gui.constants.Colors;
import org.andreidodu.nocconvert.gui.dto.ConvertionStatusDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class ConvertionStatusController {
    private static final Logger log = LogManager.getLogger(ConvertionStatusController.class);
    private final ConvertionStatusDTO convertionStatusDTO;
    private Path destinationDirectory;

    public ConvertionStatusController(ConvertionStatusDTO convertionStatusDTO) {
        this.convertionStatusDTO = convertionStatusDTO;
    }

    public void onSearchStepFinish(Path destinationDirectory, String targetFormat, List<Path> paths) {
        this.destinationDirectory = destinationDirectory;

        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.conversionFileListScrollPane().setVisible(true);
            convertionStatusDTO.scrollPanePanel().setVisible(true);

            long startTime = System.nanoTime();

            PerformantListModel modelProgress = new PerformantListModel();
            convertionStatusDTO.conversionFileList().setModel(modelProgress);
            modelProgress.setJList(convertionStatusDTO.conversionFileList());
//            PerformantListItemRenderer renderer = new PerformantListItemRenderer();
//            convertionStatusDTO.conversionFileJList().setCellRenderer(renderer);

            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;
            double durationMs = durationNs / 1_000_000.0;

            log.debug("rendering time for 0 records: {} ms", durationMs);
        });

//        List<ConversionItemDTO> list = convertPathListToDTOList(destinationDirectory, targetFormat, paths);
//        ListRendererWorker listRendererWorker = new ListRendererWorker(list, convertionStatusDTO.conversionFileList(), () -> convertionStatusDTO.guiOrchestrator().onRenderingDone());
//        listRendererWorker.execute();
    }


    public void updateList(List<ConversionItemDTO> list) {
        PerformantListModel model = (PerformantListModel) convertionStatusDTO.conversionFileList().getModel();
        model.updateElements(list);
    }

    public void newVisibleJListAndProgressBar(boolean bool) {
        SwingUtilities.invokeLater(() -> {

            PerformantListModel jListModel = new PerformantListModel();
            convertionStatusDTO.conversionFileList().setModel(jListModel);
            jListModel.setJList(convertionStatusDTO.conversionFileList());


            //convertionStatusDTO.conversionFileListScrollPane().setVisible(bool);
            // enableProgressBarPanel(bool);
        });
    }


    public void enableProgressBarPanelFromEDT(boolean bool) {
        SwingUtilities.invokeLater(() -> {
            enableProgressBarPanel(bool);
        });
    }

    private void enableProgressBarPanel(boolean bool) {
        convertionStatusDTO.progressBarAndStatusPanel().setVisible(bool);
        convertionStatusDTO.progressBar().setVisible(bool);
    }

    public void updateMainProgressBarMaxValue(int size) {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.progressBar().setMaximum(size);
        });
    }

    public void conversionDone() {
        onAllTaskComplete();
    }

    public void updateMainProgressBarProgress(int completedItems) {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.progressBar().setValue(completedItems);
        });
    }

    public void incrementMainProgressBarProgress(Integer totalNumberOfUpdates) {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.progressBar().setForeground(Colors.LIME_DARK);
            convertionStatusDTO.progressBar().setValue(convertionStatusDTO.progressBar().getValue() + Optional.ofNullable(totalNumberOfUpdates).orElseGet(() -> 1));
        });
    }

    public void startSearch() {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.scrollPanePanel().setVisible(false);
        });
    }

    public void disableProcessStatus() {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.conversionFileListScrollPane().setVisible(false);
            enableProgressBarPanel(false);
        });
    }

    public void onAllTaskComplete() {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.progressBar().setForeground(Colors.LIME_DARK);
            convertionStatusDTO.progressBar().setValue(convertionStatusDTO.progressBar().getMaximum());
        });
    }

    public void updateProgressBarColorColorToBlue() {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.progressBar().setForeground(Colors.ACCENT_BLUE);
        });
    }

    public void hideProgressBar() {
        convertionStatusDTO.progressBarAndStatusPanel().setVisible(false);
    }

    public void disableProgressBar() {
        newVisibleJListAndProgressBar(false);
    }

    public void incrementSecondaryProgressBarProgress(Integer totalNumberOfUpdates) {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.secondaryProgressBar().setForeground(Colors.LIME_DARK);
            convertionStatusDTO.secondaryProgressBar().setValue(convertionStatusDTO.secondaryProgressBar().getValue() + Optional.ofNullable(totalNumberOfUpdates).orElseGet(() -> 1));
        });
    }

    public void updateSecondaryProgressBarMaxValue(int size) {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.secondaryProgressBar().setMaximum(size);
        });
    }

    public void resetSecondaryProgressBar() {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.secondaryProgressBar().setValue(0);
        });
    }

    public void updateSecondaryProgressBarColorColorToBlue() {
        SwingUtilities.invokeLater(() -> {
            convertionStatusDTO.secondaryProgressBar().setForeground(Colors.ACCENT_BLUE);
        });
    }

    public void addPathsToTheJList(List<ConversionItemDTO> list) {
        PerformantListModel model = (PerformantListModel) convertionStatusDTO.conversionFileList().getModel();
        model.addAll(list);
    }

    public void showJListPane(boolean show) {
        SwingUtilities.invokeLater(() -> {
          convertionStatusDTO.conversionFileListScrollPane().setVisible(show);
        });
    }
}
