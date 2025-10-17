package andreidodu.nocconvert.dto;

import lombok.Builder;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Builder
public record ConvertImageRequestDTO(List<String> fileImageList,
                                     String destinationDirectory,
                                     String imageFormat,
                                     JPanel errorListJPanel,
                                     JList<String> errorListJList,
                                     Consumer<Boolean> enableUI,
                                     JFrame frame,
                                     Supplier<Boolean> isConverting,
                                     Consumer<Boolean> setConverting,
                                     JButton convertButton,
                                     ExecutorService executorService) {
}
