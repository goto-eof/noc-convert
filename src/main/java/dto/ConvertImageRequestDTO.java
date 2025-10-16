package dto;

import lombok.Builder;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

@Builder
public record ConvertImageRequestDTO(List<String> fileImageList,
                                     String destinationDirectory,
                                     String imageFormat,
                                     JList<String> errorListJList,
                                     Consumer<Boolean> enableUI,
                                     JFrame frame) {
}
