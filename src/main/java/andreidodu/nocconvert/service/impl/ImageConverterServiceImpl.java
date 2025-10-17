package andreidodu.nocconvert.service.impl;

import andreidodu.nocconvert.dto.ImageConversionResultDTO;
import andreidodu.nocconvert.service.ImageConverterService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageConverterServiceImpl implements ImageConverterService {

    public static final String PNG_FORMAT = "png";

    @Override
    public ImageConversionResultDTO convertImage(String sourceFileString, String destinationPath, String targetExtension) throws IOException {
        File sourceFile = new File(sourceFileString);

        BufferedImage image = ImageIO.read(sourceFile);

        String fileName = sourceFile.getName();
        String outputFileString = destinationPath + File.separator + renameFileExtension(fileName, targetExtension);
        File outputFile = new File(outputFileString);
        try {
            boolean status = ImageIO.write(convertToOpaqueIfNecessary(image, targetExtension), targetExtension, outputFile);
            return new ImageConversionResultDTO(sourceFileString, status);
        } catch (Exception e) {
            return new ImageConversionResultDTO(sourceFileString + " -> " + e.getMessage(), false);
        }
    }

    private String renameFileExtension(String oldFileName, String newExtension) {
        int lastIndexOf = oldFileName.lastIndexOf(".");
        return oldFileName.substring(0, lastIndexOf) + "." + newExtension;
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


}
