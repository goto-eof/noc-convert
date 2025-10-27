package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class ImageConverterServiceImpl implements ImageConverterService {
    private static final Logger log = LogManager.getLogger(ImageConverterServiceImpl.class);

    public static final String PNG_FORMAT = "png";
    public static final String ICO_FORMAT = "ico";
    private static final Object BMP_FORMAT = "nmp";

    @Override
    public void convertImage(Path sourceFile, Path destinationPath, String newFileFormat, Runnable onStart, Consumer<Float> onProgress, Runnable onDone) throws IOException {
        log.debug("Converting image from {}, format: {}", sourceFile, newFileFormat);

        if (Thread.currentThread().isInterrupted()) {
            log.error("Operation aborted 0");
            throw new ConversionManualAbortedException("Operation aborted");
        }
        onStart.run();
        onProgress.accept(1f);

        BufferedImage image = ImageIO.read(sourceFile.toFile());
        image = convertToOpaqueIfNecessary(image, newFileFormat);

        String fileName = sourceFile.getFileName().toString();

        Path outputFilePath = destinationPath.resolve(renameFileExtension(fileName, newFileFormat));
        String outputFileString = outputFilePath.toString();
        File outputFile = new File(outputFileString);
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(newFileFormat);
            if (!writers.hasNext()) {
                log.debug("No writers found for format: {}", newFileFormat);
                throw new IllegalStateException("No writer found for format: " + newFileFormat);
            }

            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
                writer.setOutput(ios);

                writer.addIIOWriteProgressListener(new IIOWriteProgressListener() {
                    @Override
                    public void imageStarted(ImageWriter source, int imageIndex) {
                        if (Thread.currentThread().isInterrupted()) {
                            log.error("Operation aborted 1");
                            throw new ConversionManualAbortedException("Operation aborted");
                        }
                    }

                    @Override
                    public void imageProgress(ImageWriter source, float percentageDone) {
                        if (Thread.currentThread().isInterrupted()) {
                            log.error("Operation aborted 2");
                            throw new ConversionManualAbortedException("Operation aborted");
                        }
                        onProgress.accept(percentageDone);
                    }

                    @Override
                    public void imageComplete(ImageWriter source) {

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
                        log.error("Operation aborted 3");
                        throw new ConversionManualAbortedException("Operation aborted");
                    }
                });


                if (Thread.currentThread().isInterrupted()) {
                    log.error("Operation aborted 4");
                    throw new ConversionManualAbortedException("Operation aborted");
                }

                writer.write(null, new IIOImage(image, null, null), null);
                onProgress.accept(99f);


            } catch (ConversionManualAbortedException e) {
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("Conversion failed, but thread was interrupted. Treating as cancelled.");
                    throw new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                }
                throw new RuntimeException(e);
            } finally {
                writer.dispose();
            }


        } catch (ConversionManualAbortedException e) {
            if (outputFile.exists()) {
                if (outputFile.delete()) {
                    log.debug("Partial file removed: {}", outputFile.getName());
                } else {
                    log.warn("Unable to remove the partial file: {}", outputFile.getName());
                }
            }
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        onDone.run();
    }

    private String renameFileExtension(String oldFileName, String newFileFormat) {
        int lastIndexOf = oldFileName.lastIndexOf(".");
        return oldFileName.substring(0, lastIndexOf) + "." + FormatExtensionMapper.getExtension(newFileFormat);
    }

    public static BufferedImage convertToOpaqueIfNecessary(BufferedImage originalImage, String targetExtension) {
        if (!Objects.equals(BMP_FORMAT, targetExtension)) {
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
        g2d.drawImage(originalImage, 0, 0, Color.WHITE, null);
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
