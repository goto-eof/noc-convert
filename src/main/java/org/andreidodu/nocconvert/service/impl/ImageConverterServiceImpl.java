package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.dto.ImageConversionResultDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.service.ImageConverterService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ImageConverterServiceImpl implements ImageConverterService {

    public static final String PNG_FORMAT = "png";

    @Override
    public ImageConversionResultDTO convertImage(Path sourceFile, Path destinationPath, String newFileFormat) throws IOException {

        BufferedImage image = ImageIO.read(sourceFile.toFile());

        String fileName = sourceFile.getFileName().toString();

        Path outputFilePath = destinationPath.resolve(renameFileExtension(fileName, newFileFormat));
        String outputFileString = outputFilePath.toString();
        File outputFile = new File(outputFileString);
        try {
            boolean status = ImageIO.write(convertToOpaqueIfNecessary(image, newFileFormat), newFileFormat, outputFile);
            return new ImageConversionResultDTO(fileName, status);
        } catch (Exception e) {
            return new ImageConversionResultDTO(fileName + " -> " + e.getMessage(), false);
        }
    }

    private String renameFileExtension(String oldFileName, String newFileFormat) {
        int lastIndexOf = oldFileName.lastIndexOf(".");
        return oldFileName.substring(0, lastIndexOf) + "." + FormatExtensionMapper.getExtension(newFileFormat);
    }

    public static BufferedImage convertToOpaqueIfNecessary(BufferedImage originalImage, String targetExtension) {
        if (List.of(PNG_FORMAT, "ico").contains(targetExtension)) {
            return originalImage;
        }

        if (originalImage.getTransparency() == java.awt.Transparency.OPAQUE) {
            return originalImage;
        }

        BufferedImage newImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        java.awt.Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        return newImage;
    }


    @Override
    public List<FormatExtensionDTO> getAvailableWriteFormatList() {
        return getFormatExtensionDTOList(ImageIO.getWriterFormatNames());
    }

    @Override
    public List<FormatExtensionDTO> getAvailableReadFormatList() {
        return getFormatExtensionDTOList(ImageIO.getReaderFormatNames());
    }

    private static List<FormatExtensionDTO> getFormatExtensionDTOList(String[] formatList) {
        List<FormatExtensionDTO> imageFormatList = new ArrayList<>();
        String[] arrayString = Arrays.stream(formatList)
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .toArray(String[]::new);
        for (String format : arrayString) {
            imageFormatList.add(new FormatExtensionDTO(format, FormatExtensionMapper.getExtension(format)));
        }
        return imageFormatList;
    }


}
