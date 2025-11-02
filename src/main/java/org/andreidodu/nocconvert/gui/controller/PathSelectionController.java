package org.andreidodu.nocconvert.gui.controller;

import lombok.Getter;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;
import org.andreidodu.nocconvert.gui.components.TextfieldDoubleButtonComponent;
import org.andreidodu.nocconvert.gui.dto.PathSelectionDTO;
import org.andreidodu.nocconvert.gui.dto.PathSelectionRawDTO;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.andreidodu.nocconvert.service.ValidationService;
import org.andreidodu.nocconvert.service.impl.FileSystemServiceImpl;
import org.andreidodu.nocconvert.service.impl.ValidationServiceImpl;
import org.andreidodu.nocconvert.util.FileSystemSupportGuiUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PathSelectionController {
    private static final Logger log = LogManager.getLogger(PathSelectionController.class);

    @Getter
    private final PathSelectionDTO pathSelectionDTO;
    private final FileSystemSupportGuiUtil fileSystemSupportGuiUtil;
    private final ValidationService validationService;
    private final FileSystemService fileSystemService;

    @Getter
    private final PathSelectionRawDTO pathSelectionRawDTO = PathSelectionRawDTO.builder().build();

    public PathSelectionController(PathSelectionDTO pathSelectionDTO) {
        this.pathSelectionDTO = pathSelectionDTO;
        fileSystemSupportGuiUtil = new FileSystemSupportGuiUtil();
        validationService = new ValidationServiceImpl();
        fileSystemService = new FileSystemServiceImpl();

        pathSelectionDTO.destinationComponent().getSecondaryButton().setVisible(false);
        pathSelectionDTO.sourceComponent().getSecondaryButton().setVisible(false);
        addEventListeners();
    }

    private void addEventListeners() {
        addEventListenersForSourceComponent();
        addEventListenersForDestinationComponent();
        addEventOpenDirectoryListeners(pathSelectionDTO.destinationComponent().getSecondaryButton(), pathSelectionDTO.guiOrchestrator()::getDestinationDirectory);
        addEventOpenDirectoryListeners(pathSelectionDTO.sourceComponent().getSecondaryButton(), pathSelectionDTO.guiOrchestrator()::getSourceDirectory);
    }

    private void addEventOpenDirectoryListeners(JButton button, Supplier<Path> supplier) {
        button.addActionListener(e -> {
            Path destinationDirectory = supplier.get();
            if (destinationDirectory == null) {
                return;
            }
            if (destinationDirectory != null && !Files.exists(destinationDirectory)) {
                JOptionPane.showMessageDialog(null, "Directory does not exist.", "Error", JOptionPane.ERROR_MESSAGE);
                return;

            }

            File directory = destinationDirectory.toFile();

            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(directory);
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        });
    }

    private void addEventListenersForSourceComponent() {
        TextfieldDoubleButtonComponent sourceComponent = pathSelectionDTO.sourceComponent();
        addBrowseDirectoryEventListener(sourceComponent, pathSelectionRawDTO::setSourceDirectory, this::isValidPath, () -> {
            sourceComponent.getSecondaryButton().setVisible(true);
        });
    }

    private boolean isValidPath(Path path) {
        boolean isValidPath = validationService.isValidaDirectory(path);
        if (!isValidPath) {
            JOptionPane.showMessageDialog(pathSelectionDTO.guiOrchestrator(), "Invalid Source path", "ERROR", JOptionPane.ERROR_MESSAGE);
        }
        return isValidPath;
    }

    private void addEventListenersForDestinationComponent() {
        TextfieldDoubleButtonComponent destinationComponent = pathSelectionDTO.destinationComponent();
        addBrowseDirectoryEventListener(destinationComponent, pathSelectionRawDTO::setDestinationDirectory, this::isAllowOverrideIfNecessary, () -> {
            destinationComponent.getSecondaryButton().setVisible(true);
        });
    }

    private boolean isAllowOverrideIfNecessary(Path path) {
        if (!fileSystemService.containsAtLeaseOneFile(path)) {
            return true;
        }
        return isUserWantToContinue();
    }

    private Boolean isUserWantToContinue() {
        int response = JOptionPane.showConfirmDialog(pathSelectionDTO.guiOrchestrator(),
                "The destination directory contains at least one file. New files will be renamed if the same filename will be find in the directory.\nDo you want to continue?",
                "WARNING!",
                JOptionPane.YES_NO_OPTION);

        return response == JOptionPane.YES_OPTION;
    }

    private void addBrowseDirectoryEventListener(TextfieldButtonComponent component, Consumer<Path> pathConsumer, Predicate<Path> isAllowOverrideIfNecessary, Runnable callback) {
        component.getButton()
                .addActionListener(e -> fileSystemSupportGuiUtil.selectDirectory()
                        .ifPresent(text -> {
                            Path path = Path.of(text);
                            if (!isAllowOverrideIfNecessary.test(path)) {
                                return;
                            }
                            pathConsumer.accept(path);
                            component.getTextField().setText(path.getFileName().toString());
                            callback.run();
                        })
                );
    }

    public List<String> retrieveValidationErrorsIfExists() {
        List<String> errors = new ArrayList<>();
        validationService.isValidatePath(pathSelectionRawDTO.getSourceDirectory()).ifPresent(text -> errors.add("Source directory: " + text));
        validationService.isValidatePath(pathSelectionRawDTO.getDestinationDirectory()).ifPresent(text -> errors.add("Destination directory: " + text));
        return errors;
    }

    public void enableButtons(boolean bool) {
        SwingUtilities.invokeLater(() -> {
            pathSelectionDTO.sourceComponent().setEnabled(bool);
            pathSelectionDTO.destinationComponent().setEnabled(bool);
        });
    }

}
