package org.andreidodu.nocconvert.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.andreidodu.nocconvert.gui.components.ListItemRenderer;

import javax.swing.*;
import java.nio.file.Path;

@Getter
@Setter
@Builder
public class ConversionItemDTO {

    private String fileName;
    private String fileSize;
    private float progressPercentage;
    private String targetFormat;
    private ConversionStatus status;
    private Path path;

    private JList<ListItemRenderer> conversionFileList;
}
