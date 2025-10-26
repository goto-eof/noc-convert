package org.andreidodu.nocconvert.gui.controller.worker;

import org.andreidodu.nocconvert.dto.ConvertImageRequestDTO;
import org.andreidodu.nocconvert.service.ImageConverterFacadeService;
import org.andreidodu.nocconvert.service.impl.ImageConverterFacadeServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.List;

public class ConvertImageListSwingWorker extends SwingWorker<List<String>, Void> {
    private static final Logger log = LogManager.getLogger(ConvertImageListSwingWorker.class);
    private final ImageConverterFacadeService imageConverterFacadeService;
    private final ConvertImageRequestDTO convertImageRequestDTO;

    public ConvertImageListSwingWorker(ConvertImageRequestDTO convertImageRequestDTO) {
        super();
        this.imageConverterFacadeService = new ImageConverterFacadeServiceImpl();
        this.convertImageRequestDTO = convertImageRequestDTO;
    }

    @Override
    protected List<String> doInBackground() {
        SwingUtilities.invokeLater(() -> {
            convertImageRequestDTO.setConverting().accept(true);
            convertImageRequestDTO.convertButton().setText("Stop");
            convertImageRequestDTO.enableUI().accept(false);
            convertImageRequestDTO.errorListJPanel().setVisible(false);
            convertImageRequestDTO.frame().revalidate();
            convertImageRequestDTO.frame().pack();
        });

        try {
            imageConverterFacadeService.convertFileListToCustomFormatMultithreaded(convertImageRequestDTO.executorService(), convertImageRequestDTO.fileImageList(), convertImageRequestDTO.destinationDirectory(), convertImageRequestDTO.imageFormat(), this::onProgress, this::onStart, this::onComplete);
            return List.of();
        } catch (Exception e) {
            log.error(e.getMessage());
            return List.of("premature interruption made by the user");
        }
    }

    public void onProgress(float progress) {

    }

    public void onStart() {
        SwingUtilities.invokeLater(() -> {
        });
    }

    public void onComplete() {
        SwingUtilities.invokeLater(() -> {
        });
    }

    @Override
    protected void done() {
        try {
            List<String> failedFilenameList = get();
            String message = String.format("Operation result: %s", failedFilenameList.isEmpty() ? "Success!" : "failure");

            if (!failedFilenameList.isEmpty()) {
                convertImageRequestDTO.errorListJPanel().setVisible(true);
                convertImageRequestDTO.frame().revalidate();
                convertImageRequestDTO.frame().pack();
            }

            DefaultListModel<String> model = new DefaultListModel<>();
            failedFilenameList.forEach(model::addElement);
            convertImageRequestDTO.errorListJList().setModel(model);

            JOptionPane.showMessageDialog(null,
                    message,
                    "Operation Result",
                    JOptionPane.INFORMATION_MESSAGE);

            convertImageRequestDTO.setConverting().accept(false);
            convertImageRequestDTO.convertButton().setText("Start");
            convertImageRequestDTO.enableUI().accept(true);

        } catch (Exception ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        }
    }
}