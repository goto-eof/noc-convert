package org.andreidodu.nocconvert.controller;

import org.andreidodu.nocconvert.dto.PathSelectionDTO;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;
import org.andreidodu.nocconvert.util.FileSystemSupportGuiUtil;

public class PathSelectionController {
    private final PathSelectionDTO pathSelectionDTO;
    private final FileSystemSupportGuiUtil fileSystemSupportGuiUtil;


    public PathSelectionController(PathSelectionDTO pathSelectionDTO) {
        this.pathSelectionDTO = pathSelectionDTO;
        fileSystemSupportGuiUtil = new FileSystemSupportGuiUtil();

        addEventListeners();
    }

    private void addEventListeners() {
        addEventListenersForSourceComponent();
        addEventListenersForDestinationComponent();
    }

    private void addEventListenersForSourceComponent() {
        TextfieldButtonComponent sourceComponent = pathSelectionDTO.sourceComponent();
        addBrowseDirectoryEventListener(sourceComponent);
    }

    private void addEventListenersForDestinationComponent() {
        TextfieldButtonComponent destinationComponent = pathSelectionDTO.destinationComponent();
        addBrowseDirectoryEventListener(destinationComponent);
    }

    private void addBrowseDirectoryEventListener(TextfieldButtonComponent component) {
        component.getButton()
                .addActionListener(e -> fileSystemSupportGuiUtil.selectDirectory()
                        .ifPresent(text -> component.getTextField().setText(text))
                );
    }

}
