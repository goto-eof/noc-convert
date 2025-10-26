package org.andreidodu.nocconvert.gui.controller;

import lombok.Getter;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class PathSelectionController {
    private static final Logger log = LogManager.getLogger(PathSelectionController.class);

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

        addEventListeners();
    }

    private void addEventListeners() {
        addEventListenersForSourceComponent();
        addEventListenersForDestinationComponent();
    }

    private void addEventListenersForSourceComponent() {
        TextfieldButtonComponent sourceComponent = pathSelectionDTO.sourceComponent();
        addBrowseDirectoryEventListener(sourceComponent, pathSelectionRawDTO::setSourceDirectory, this::isValidPath);
    }

    private boolean isValidPath(Path path) {
        boolean isValidPath = validationService.isValidaDirectory(path);
        if (!isValidPath) {
            JOptionPane.showMessageDialog(pathSelectionDTO.guiOrchestrator(), "Invalid Source path", "ERROR", JOptionPane.ERROR_MESSAGE);
        }
        return isValidPath;
    }

    private void addEventListenersForDestinationComponent() {
        TextfieldButtonComponent destinationComponent = pathSelectionDTO.destinationComponent();
        addBrowseDirectoryEventListener(destinationComponent, pathSelectionRawDTO::setDestinationDirectory, this::isAllowOverrideIfNecessary);
    }

    private boolean isAllowOverrideIfNecessary(Path path) {
        if (!fileSystemService.containsAtLeaseOneFile(path)) {
            return true;
        }
        return isUserWantToContinue();
    }

    private Boolean isUserWantToContinue() {
        int response = JOptionPane.showConfirmDialog(pathSelectionDTO.guiOrchestrator(),
                "The destination directory contains at least one file. Some files or all files in the destination directory could be override.\nDo you want to continue?",
                "WARNING!",
                JOptionPane.YES_NO_OPTION);

        return response == JOptionPane.YES_OPTION;
    }

    private void addBrowseDirectoryEventListener(TextfieldButtonComponent component, Consumer<Path> pathConsumer, Predicate<Path> isAllowOverrideIfNecessary) {
        component.getButton()
                .addActionListener(e -> fileSystemSupportGuiUtil.selectDirectory()
                        .ifPresent(text -> {
                            Path path = Path.of(text);
                            if (!isAllowOverrideIfNecessary.test(path)) {
                                return;
                            }
                            pathConsumer.accept(path);
                            component.getTextField().setText(path.getFileName().toString());
                        })
                );
    }


    public List<String> retrieveValidationErrorsIfExists() {
        List<String> errors = new ArrayList<>();
        validationService.isValidatePath(pathSelectionRawDTO.getSourceDirectory()).ifPresent(text -> errors.add("Source directory: " + text));
        validationService.isValidatePath(pathSelectionRawDTO.getDestinationDirectory()).ifPresent(text -> errors.add("Destination directory: " + text));
        return errors;
    }


    public void setEnableComponents(boolean bool) {
        SwingUtilities.invokeLater(() -> {
            pathSelectionDTO.sourceComponent().setEnabled(bool);
            pathSelectionDTO.destinationComponent().setEnabled(bool);
        });
    }
}
