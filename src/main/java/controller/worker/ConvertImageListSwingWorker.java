package controller.worker;

import dto.ConvertImageRequestDTO;
import dto.ImageConversionResultDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.ImageConverterFacadeService;
import service.impl.ImageConverterFacadeServiceImpl;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class ConvertImageListSwingWorker extends SwingWorker<List<String>, Void> {
    private static final Logger log = LogManager.getLogger(ConvertImageListSwingWorker.class);

    private final ImageConverterFacadeService imageConverterFacadeService;
    private final List<String> fileImageList;
    private final String destinationDirectory;
    private final String imageFormat;

    private final JPanel errorListJPanel;
    private final JList<String> errorListJList;
    private final Consumer<Boolean> enableUI;
    private final JFrame frame;

    public ConvertImageListSwingWorker(ConvertImageRequestDTO convertImageRequestDTO) {
        super();
        this.imageConverterFacadeService = new ImageConverterFacadeServiceImpl();
        this.fileImageList = convertImageRequestDTO.fileImageList();
        this.destinationDirectory = convertImageRequestDTO.destinationDirectory();
        this.imageFormat = convertImageRequestDTO.imageFormat();
        this.errorListJPanel = convertImageRequestDTO.errorListJPanel();
        this.enableUI = convertImageRequestDTO.enableUI();
        this.frame = convertImageRequestDTO.frame();
        this.errorListJList = convertImageRequestDTO.errorListJList();
    }

    @Override
    protected List<String> doInBackground() {
        enableUI.accept(false);
        return imageConverterFacadeService.convertFileListToCustomFormatMultithreaded(fileImageList, destinationDirectory, imageFormat)
                .stream()
                .filter(record -> !record.status())
                .map(ImageConversionResultDTO::filename)
                .toList();
    }

    @Override
    protected void done() {
        try {
            List<String> failedFilenameList = get();
            String message = String.format("Operation result: %s", failedFilenameList.isEmpty() ? "Success!" : "failure");

            if (!failedFilenameList.isEmpty()) {
                errorListJPanel.setVisible(true);
                frame.revalidate();
                frame.pack();
            }

            DefaultListModel<String> model = new DefaultListModel<>();
            failedFilenameList.forEach(model::addElement);
            errorListJList.setModel(model);

            JOptionPane.showMessageDialog(null,
                    message,
                    "Operation Result",
                    JOptionPane.INFORMATION_MESSAGE);
            enableUI.accept(true);

        } catch (Exception ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        }
    }
};