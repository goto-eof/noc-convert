package org.andreidodu.nocconvert.controller;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.PathSelectionDTO;
import org.andreidodu.nocconvert.dto.PathSelectionRawDTO;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;
import org.andreidodu.nocconvert.util.FileSystemSupportGuiUtil;

import java.nio.file.Path;
import java.util.function.Consumer;

public class PathSelectionController {
    private final PathSelectionDTO pathSelectionDTO;
    private final FileSystemSupportGuiUtil fileSystemSupportGuiUtil;

    @Getter
    private final PathSelectionRawDTO pathSelectionRawDTO = PathSelectionRawDTO.builder().build();

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
        addBrowseDirectoryEventListener(sourceComponent, pathSelectionRawDTO::setSourceDirectory);
    }

    private void addEventListenersForDestinationComponent() {
        TextfieldButtonComponent destinationComponent = pathSelectionDTO.destinationComponent();
        addBrowseDirectoryEventListener(destinationComponent, pathSelectionRawDTO::setDestinationDirectory);
    }

    private void addBrowseDirectoryEventListener(TextfieldButtonComponent component, Consumer<Path> pathConsumer) {
        component.getButton()
                .addActionListener(e -> fileSystemSupportGuiUtil.selectDirectory()
                        .ifPresent(text -> {
                            Path path = Path.of(text);
                            pathConsumer.accept(path);
                            component.getTextField().setText(path.getFileName().toString());
                        })
                );
    }


}
