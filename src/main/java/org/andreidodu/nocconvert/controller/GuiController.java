package org.andreidodu.nocconvert.controller;

import lombok.Getter;
import lombok.Setter;
import org.andreidodu.nocconvert.controller.worker.ConvertImageListSwingWorker;
import org.andreidodu.nocconvert.dto.ConvertImageRequestDTO;
import org.andreidodu.nocconvert.dto.GUIControllerComponentsDTO;
import org.andreidodu.nocconvert.dto.ImageFormatDTO;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.andreidodu.nocconvert.service.ImageConverterFacadeService;
import org.andreidodu.nocconvert.service.impl.FileSystemServiceImpl;
import org.andreidodu.nocconvert.service.impl.ImageConverterFacadeServiceImpl;
import org.andreidodu.nocconvert.util.CustomThreadFactory;
import org.andreidodu.nocconvert.util.FileSystemSupportGuiUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class GuiController {
    private static final Logger log = LogManager.getLogger(GuiController.class);

    private final JTextField sourceDirectoryTextField;
    private final JButton chooseSourceDirectoryButton;
    private final JList<String> sourceDirectoryFilesList;

    private final JTextField destinationDirectoryTextField;
    private final JButton chooseDestinationDirectoryButton;

    private final JButton convertButton;
    private final JList<String> errorsJList;
    private final JComboBox<ImageFormatDTO> targetFileFormatComboBox;
    private final JFrame frame;
    private final JPanel errorListPanel;
    private final JLabel fileFormatsJLabel;

    private final ImageConverterFacadeService imageConverterFacadeService;
    private final FileSystemSupportGuiUtil fileSystemSupportGuiUtil;
    private final FileSystemService fileSystemService;

    @Getter
    @Setter
    private volatile boolean isConverting = false;

    private volatile ExecutorService conversionExecutorPoolService;


    public GuiController(GUIControllerComponentsDTO guiControllerComponentsDTO) {
        this.sourceDirectoryTextField = guiControllerComponentsDTO.sourceDirectoryTextField();
        this.chooseSourceDirectoryButton = guiControllerComponentsDTO.chooseSourceDirectoryButton();
        this.sourceDirectoryFilesList = guiControllerComponentsDTO.sourceDirectoryFilesList();

        this.destinationDirectoryTextField = guiControllerComponentsDTO.destinationDirectoryTextField();
        this.chooseDestinationDirectoryButton = guiControllerComponentsDTO.chooseDestinationDirectoryButton();

        this.convertButton = guiControllerComponentsDTO.convertButton();
        this.errorsJList = guiControllerComponentsDTO.errorsJList();
        this.targetFileFormatComboBox = guiControllerComponentsDTO.targetFileFormatComboBox();
        this.frame = guiControllerComponentsDTO.frame();
        this.errorListPanel = guiControllerComponentsDTO.errorListPanel();
        this.fileFormatsJLabel = guiControllerComponentsDTO.fileFormatsJLabel();

        this.imageConverterFacadeService = new ImageConverterFacadeServiceImpl();
        this.fileSystemSupportGuiUtil = new FileSystemSupportGuiUtil();
        this.fileSystemService = new FileSystemServiceImpl();

        addSourceDirectoryChooseButtonEventListener();
        addDestinationDirectoryChooseButtonEventListener();
        addConvertButtonActionListener();

        this.destinationDirectoryTextField.setText(System.getProperty("user.home") + "/noc-convert");

        updateFileFormatJLabel();

    }

    private void updateFileFormatJLabel() {
        String[] readerFormatNames = ImageIO.getReaderFormatNames();
        List<String> fileFormatStream = Stream.of(readerFormatNames)
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .toList();
        List<String> filteredFileFormatList = new ArrayList<>();

        int i = 1;
        for (String fileFormat : fileFormatStream) {
            if (i % 23 == 0) {
                i = 1;
                filteredFileFormatList.add("<br/>");
            }
            filteredFileFormatList.add(fileFormat);
            i++;
        }

        Optional.of("<html>accepted formats: " + String.join(" ",
                        filteredFileFormatList
                ) + "</html>")
                .ifPresent(fileFormats -> fileFormatsJLabel.setText(fileFormats));
    }

    private void addConvertButtonActionListener() {
        convertButton.addActionListener(e -> {
            if (isConverting) {
                stopConversion();
                return;
            }

            if (!validateInput()) {
                return;
            }
            startConversion();
        });
    }

    private boolean validateInput() {
        if (sourceDirectoryTextField.getText().isEmpty()) {
            showError("Please choose a Source directory");
            return false;
        }
        if (destinationDirectoryTextField.getText().isEmpty()) {
            showError("Please choose a Destination directory");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                    message,
                    "Validation Result",
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    private synchronized void stopConversion() {
        if (conversionExecutorPoolService != null && !conversionExecutorPoolService.isTerminated()) {
            conversionExecutorPoolService.shutdownNow();
            setConverting(false);
            convertButton.setText("Start");
            enableUI(true);
        }

    }

    private synchronized void startConversion() {
        try {

            setConverting(true);

            SwingUtilities.invokeLater(() -> {
                DefaultListModel<String> model = new DefaultListModel<>();
                sourceDirectoryFilesList.setModel(model);
                errorsJList.setModel(model);
            });

            String sourcePath = sourceDirectoryTextField.getText();
            List<String> fileImageListToConvert = imageConverterFacadeService.getAllFilesDirectoryByExtension(sourcePath,
                    Arrays.stream(ImageIO.getReaderFormatNames())
                            .map(String::toLowerCase)
                            .distinct()
                            .sorted()
                            .toList()
            );

            SwingUtilities.invokeLater(() -> {
                DefaultListModel<String> model = new DefaultListModel<>();
                fileImageListToConvert.forEach(model::addElement);
                sourceDirectoryFilesList.setModel(model);
            });

            String destinationPath = destinationDirectoryTextField.getText();


            if (targetFileFormatComboBox.getSelectedItem() == null || ((ImageFormatDTO) targetFileFormatComboBox.getSelectedItem()).format() == null || ((ImageFormatDTO) targetFileFormatComboBox.getSelectedItem()).format().isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null,
                            "Invalid target file format!",
                            "Validation Result",
                            JOptionPane.ERROR_MESSAGE);
                });
                return;
            }
            String targetFormat = ((ImageFormatDTO) targetFileFormatComboBox.getSelectedItem()).format();


            if (conversionExecutorPoolService == null || conversionExecutorPoolService.isTerminated()) {
                conversionExecutorPoolService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new CustomThreadFactory());
            }


            ConvertImageRequestDTO guiControllerInput = ConvertImageRequestDTO.builder()
                    .fileImageList(fileImageListToConvert)
                    .destinationDirectory(destinationPath)
                    .imageFormat(targetFormat)
                    .errorListJPanel(errorListPanel)
                    .enableUI(this::enableUI)
                    .frame(frame)
                    .errorListJList(errorsJList)
                    .isConverting((this::isConverting))
                    .setConverting(this::setConverting)
                    .convertButton(convertButton)
                    .executorService(conversionExecutorPoolService)
                    .build();

            new ConvertImageListSwingWorker(guiControllerInput)
                    .execute();

        } catch (Exception ex) {
            log.error(ex);
            throw new RuntimeException(ex);
        }
    }

    private void enableUI(boolean enableFlag) {
        destinationDirectoryTextField.setEnabled(enableFlag);
        sourceDirectoryTextField.setEnabled(enableFlag);
        chooseDestinationDirectoryButton.setEnabled(enableFlag);
        chooseSourceDirectoryButton.setEnabled(enableFlag);
        // convertButton.setEnabled(enableFlag);
        targetFileFormatComboBox.setEnabled(enableFlag);

        frame.pack();
    }

    private void addDestinationDirectoryChooseButtonEventListener() {
        chooseDestinationDirectoryButton.addActionListener(e -> fileSystemSupportGuiUtil.selectDirectory()
                .ifPresent(directory -> {
                    destinationDirectoryTextField.setText(directory);
                    if (fileSystemService.containsAtLeaseOneFile(directory)) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null,
                                    "The destination directory contains at least one file. All files in the destination directory will be override!",
                                    "WARNING!",
                                    JOptionPane.WARNING_MESSAGE);
                        });
                    }
                }));
    }

    private void addSourceDirectoryChooseButtonEventListener() {
        chooseSourceDirectoryButton.addActionListener(e -> fileSystemSupportGuiUtil.selectDirectory()
                .ifPresent(sourceDirectoryTextField::setText)
        );
    }


}
