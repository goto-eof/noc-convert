package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class ImageConverterServiceImpl implements ImageConverterService {
    private static final Logger log = LogManager.getLogger(ImageConverterServiceImpl.class);

    public static final String PNG_FORMAT = "png";
    public static final String ICO_FORMAT = "ico";

    @Override
    public void convertImage(Path sourceFile, Path destinationPath, String newFileFormat, Runnable onStart, Consumer<Float> onProgress, Runnable onDone) throws IOException {

        BufferedImage image = ImageIO.read(sourceFile.toFile());
        image = convertToOpaqueIfNecessary(image, newFileFormat);

        String fileName = sourceFile.getFileName().toString();

        Path outputFilePath = destinationPath.resolve(renameFileExtension(fileName, newFileFormat));
        String outputFileString = outputFilePath.toString();
        File outputFile = new File(outputFileString);
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(newFileFormat);
            if (!writers.hasNext()) {
                throw new IllegalStateException("No writer found for format: " + newFileFormat);
            }

            ImageWriter writer = writers.next();

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
                writer.setOutput(ios);

                writer.addIIOWriteProgressListener(new IIOWriteProgressListener() {
                    @Override
                    public void imageStarted(ImageWriter source, int imageIndex) {
                        onStart.run();
                    }

                    @Override
                    public void imageProgress(ImageWriter source, float percentageDone) {
                        onProgress.accept(percentageDone);
                    }

                    @Override
                    public void imageComplete(ImageWriter source) {
                        onDone.run();
                    }

                    @Override
                    public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {
                    }

                    @Override
                    public void thumbnailProgress(ImageWriter source, float percentageDone) {
                    }

                    @Override
                    public void thumbnailComplete(ImageWriter source) {
                    }

                    @Override
                    public void writeAborted(ImageWriter source) {
                        log.error("Operation aborted");
                        throw new RuntimeException("Operation aborted");
                    }
                });

                ImageWriteParam customWriteParam = writer.getDefaultWriteParam();
                writer.write(null, new IIOImage(image, null, null), customWriteParam);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            } finally {
                writer.dispose();
            }


        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String renameFileExtension(String oldFileName, String newFileFormat) {
        int lastIndexOf = oldFileName.lastIndexOf(".");
        return oldFileName.substring(0, lastIndexOf) + "." + FormatExtensionMapper.getExtension(newFileFormat);
    }

    public static BufferedImage convertToOpaqueIfNecessary(BufferedImage originalImage, String targetExtension) {
        if (List.of(PNG_FORMAT, ICO_FORMAT).contains(targetExtension)) {
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
