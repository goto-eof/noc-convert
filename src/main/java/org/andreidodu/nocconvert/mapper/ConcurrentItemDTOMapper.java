package org.andreidodu.nocconvert.mapper;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.IntWrapper;
import org.andreidodu.nocconvert.helper.FileUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConcurrentItemDTOMapper {

    public static List<ConversionItemDTO> convertPathListToDTOList(Path destPath, String targetFormat, List<Path> paths, IntWrapper intWrapper) {
        List<ConversionItemDTO> list = new ArrayList<>();
        for (Path path : paths) {
            ConversionItemDTO item = ConversionItemDTO.builder()
                    .index(intWrapper.incrementAndGetValue())
                    .targetFormat(targetFormat)
                    .targetExtension(targetFormat)
                    .sourceFile(path)
                    .fileName(prepareFileName(path))
                    .status(ConversionStatus.QUEUED)
                    .progressPercentage(0f)
                    .sourceFile(path)
                    .destinationDirectory(destPath)
                    .fileSize(FileUtil.getHumanableFileSize(path.toFile()))
                    .build();
            list.add(item);
        }
        paths.clear();
        return list;
    }

    private static String prepareFileName(Path path) {
        String filename = path.getFileName().toString();
        int maxLength = 50;
        if (filename.length() > maxLength) {
            filename = filename.substring(0, maxLength) + "...";
        }
        return filename;
    }
}
