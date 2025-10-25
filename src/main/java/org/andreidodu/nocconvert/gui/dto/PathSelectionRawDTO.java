package org.andreidodu.nocconvert.gui.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Builder
@Getter
@Setter
public class PathSelectionRawDTO {
    private Path sourceDirectory;
    private Path destinationDirectory;

}
