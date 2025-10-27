package org.andreidodu.nocconvert.service.impl;

import lombok.Setter;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ImageConverterServiceImpl implements ImageConverterService {
    private static final Logger log = LogManager.getLogger(ImageConverterServiceImpl.class);

    private static final List<String> NO_ALPHA_FORMAT = List.of("bmp", "wbmp", "jpg", "jpeg");
    private ImageWriter writer;
    private ImageOutputStream currentOutputStream;
    private ImageInputStream imageInputStream;
    int totalPercentage = 0;
    @Setter
    private boolean canceled = false;
    private static final ExecutorService CPU_POOL = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    @Override
    public void convertImage(Path sourceFile, Path destinationPath, String newFileFormat, Runnable onStart, Consumer<Float> onProgress, Runnable onDone) throws IOException {
        if (canceled) {
            return;
        }
        log.debug("Converting image from {}, format: {}", sourceFile, newFileFormat);

        interruptIfNecessary();
        onStart.run();
        onProgress.accept(1f);

        File file = sourceFile.toFile();
        imageInputStream = ImageIO.createImageInputStream(file);

        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
        if (!readers.hasNext()) {
            throw new IOException("No ImageReader found for: " + file);
        }

        int[] readPercentage = new int[1];
        readPercentage[0] = 0;

        ImageReader reader = readers.next();
        reader.setInput(imageInputStream, true, true);
        reader.addIIOReadProgressListener(new IIOReadProgressListener() {
            @Override
            public void sequenceStarted(ImageReader source, int minIndex) {
            }

            @Override
            public void sequenceComplete(ImageReader source) {
            }

            @Override
            public void imageStarted(ImageReader source, int imageIndex) {
                readPercentage[0] = 1;
            }

            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                readPercentage[0] = (int) percentageDone;

            }

            @Override
            public void imageComplete(ImageReader source) {
                readPercentage[0] = 100;
            }

            @Override
            public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
            }

            @Override
            public void thumbnailProgress(ImageReader source, float percentageDone) {
            }

            @Override
            public void thumbnailComplete(ImageReader source) {
            }

            @Override
            public void readAborted(ImageReader source) {
                totalPercentage = 0;
            }
        });

        interruptIfNecessary();
        BufferedImage image;
        try {
            image = CPU_POOL.submit(() ->
                    convertToOpaqueIfNecessary(reader.read(0), newFileFormat)
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        interruptIfNecessary();
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

            writer = writers.next();
            try {
                currentOutputStream = ImageIO.createImageOutputStream(outputFile);
                writer.setOutput(currentOutputStream);
                writer.addIIOWriteProgressListener(new IIOWriteProgressListener() {
                    @Override
                    public void imageStarted(ImageWriter source, int imageIndex) {
                        if (Thread.currentThread().isInterrupted()) {
                            log.error("Operation aborted 1");
                        }
                    }

                    @Override
                    public void imageProgress(ImageWriter source, float percentageDone) {
                        if (Thread.currentThread().isInterrupted()) {
                            log.error("Operation aborted 2");
                        }

                        onProgress.accept(readPercentage[0] / 2 + percentageDone / 2);
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
                    }
                });


                if (Thread.currentThread().isInterrupted()) {
                    log.error("Operation aborted 4");
                    throw new ConversionManualAbortedException("Operation aborted");
                }

                writer.write(null, new IIOImage(image, null, null), null);
                onProgress.accept(100f);
                onDone.run();


            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                }
                throw e;
            } catch (ConversionManualAbortedException e) {
                throw e;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                deleteBrokenFileIfExists(outputFile);
                if (Thread.currentThread().isInterrupted()) {
                    log.warn("Conversion failed, but thread was interrupted. Treating as cancelled.");
                    throw new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                }
                throw new RuntimeException(e);
            }

        } catch (ConversionManualAbortedException e) {
            deleteBrokenFileIfExists(outputFile);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            deleteBrokenFileIfExists(outputFile);
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                writer.dispose();
            }

            closeAllStreams();


        }

        onDone.run();
    }

    private static void deleteBrokenFileIfExists(File outputFile) {
        if (outputFile.exists()) {
            if (outputFile.delete()) {
                log.debug("Partial file removed: {}", outputFile.getName());
            } else {
                log.warn("Unable to remove the partial file: {}", outputFile.getName());
            }
        }
    }

    private static void interruptIfNecessary() {
        if (Thread.currentThread().isInterrupted()) {
            log.error("Operation aborted 0");
            throw new ConversionManualAbortedException("Operation aborted");
        }
    }

    private String renameFileExtension(String oldFileName, String newFileFormat) {
        int lastIndexOf = oldFileName.lastIndexOf(".");
        return oldFileName.substring(0, lastIndexOf) + "." + FormatExtensionMapper.getExtension(newFileFormat);
    }

    public static BufferedImage convertToOpaqueIfNecessary(BufferedImage originalImage, String targetExtension) {
        if (!NO_ALPHA_FORMAT.contains(targetExtension)) {
            return originalImage;
        }

//        if (originalImage.getTransparency() == java.awt.Transparency.OPAQUE) {
//            return originalImage;
//        }

        BufferedImage newImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                calculateType(targetExtension)
        );

        java.awt.Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, Color.WHITE, null);
        g2d.dispose();

        return newImage;
    }

    private static int calculateType(String targetExtension) {
        if ("bmp".equalsIgnoreCase(targetExtension)) {
            return BufferedImage.TYPE_INT_RGB;
        } else if ("wbmp".equalsIgnoreCase(targetExtension)) {
            return BufferedImage.TYPE_BYTE_BINARY;
        }
        return BufferedImage.TYPE_INT_RGB;
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

    @Override
    public void closeAllStreams() {
        setCanceled(true);
        if (imageInputStream != null) {
            try {
                log.debug("Closing image input stream");
                imageInputStream.close();
            } catch (IOException ignored) {
            }
        }
        if (currentOutputStream != null) {
            try {
                log.debug("Closed image output stream.");
                currentOutputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

}
