package org.andreidodu.nocconvert.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.andreidodu.nocconvert.gui.components.ListItemRenderer;

import javax.swing.*;

@Getter
@Setter
@Builder
public class ConversionItemDTO {

    private String fileName;
    private String fileSize;
    private float progressPercentage;
    private String targetFormat;
    private ConversionStatus status;

    private JList<ListItemRenderer> conversionFileList;
}
