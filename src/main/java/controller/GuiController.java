package controller;

import controller.worker.ConvertImageListSwingWorker;
import dto.ConvertImageRequestDTO;
import dto.GUIControllerComponentsDTO;
import dto.ImageFormatDTO;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.FileSystemService;
import service.ImageConverterFacadeService;
import service.impl.FileSystemServiceImpl;
import service.impl.ImageConverterFacadeServiceImpl;
import util.CustomThreadFactory;
import util.FileSystemSupportGuiUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        this.imageConverterFacadeService = new ImageConverterFacadeServiceImpl();
        this.fileSystemSupportGuiUtil = new FileSystemSupportGuiUtil();
        this.fileSystemService = new FileSystemServiceImpl();

        // TODO remove me
        sourceDirectoryTextField.setText("/home/andrei/Pictures");
        destinationDirectoryTextField.setText("/home/andrei/Desktop/test");

        addSourceDirectoryChooseButtonEventListener();
        addDestinationDirectoryChooseButtonEventListener();
        addConvertButtonActionListener();

    }

    private void addConvertButtonActionListener() {
        convertButton.addActionListener(e -> {
            if (isConverting) {
                stopConversion();
                return;
            }
            startConversion();
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
