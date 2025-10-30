package org.andreidodu.nocconvert.service.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.andreidodu.nocconvert.dto.ImageHeaderDTO;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.exception.InvalidFileFormatException;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static org.andreidodu.nocconvert.util.FileUtil.deleteBrokenFileIfExists;
import static org.andreidodu.nocconvert.util.ImageUtil.*;

public class ImageConverterServiceImpl implements ImageConverterService {
    private static final Logger log = LogManager.getLogger(ImageConverterServiceImpl.class);
    public static final String WEBP_FORMAT = "webp";
    int totalPercentage = 0;
    @Setter
    private boolean canceled = false;
    private final ExecutorService platformExecutorService;

    public ImageConverterServiceImpl(ExecutorService platformExecutorService) {
        this.platformExecutorService = platformExecutorService;
    }

    @Override
    public void convertImage(Path sourceFile, Path destinationPath, String newFileFormat, Runnable onStart, Consumer<Float> onProgress) throws IOException {
        if (canceled) {
            throw new ConversionManualAbortedException();
        }

        log.debug("Converting image {} from {} to {}", sourceFile.getFileName(), sourceFile, newFileFormat);

        interruptIfNecessary();

        onStart.run();
        onProgress.accept(1f);

        File file = sourceFile.toFile();
        ImageHeaderDTO sourceFileHeaders = getImageHeaders(file);

        String sourceFileFormat = sourceFileHeaders.format();
        List<String> validInputFormatListInLowerCase = Arrays.stream(ImageIO.getReaderFormatNames()).toList();
        validateInputImage(sourceFile, sourceFileFormat, validInputFormatListInLowerCase);

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(file)) {


            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

            validateReader(readers, file);

            TotalReadPercentageDTO totalReadPercentageDTO = TotalReadPercentageDTO.builder().build();

            BufferedImage image = readImage(imageInputStream, readers, totalReadPercentageDTO);

            interruptIfNecessary();

            image = preprocessImage(sourceFile, newFileFormat, image, platformExecutorService);

            interruptIfNecessary();

            File outputFile = calculateOutputFile(sourceFile, destinationPath, newFileFormat);

            writeImageOuter(newFileFormat, onProgress, outputFile, totalReadPercentageDTO, image);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static void validateReader(Iterator<ImageReader> readers, File file) {
        if (!readers.hasNext()) {
            throw new RuntimeException("No ImageReader found for: " + file);
        }
    }

    private static File calculateOutputFile(Path sourceFile, Path destinationPath, String newFileFormat) {
        String fileName = sourceFile.getFileName().toString();
        Path outputFilePath = destinationPath.resolve(renameFileExtension(fileName, newFileFormat));
        int i = 1;
        while (outputFilePath.toFile().exists()) {
            fileName = "copy-" + (i++) + "-of-" + sourceFile.getFileName().toString();
            outputFilePath = destinationPath.resolve(renameFileExtension(fileName, newFileFormat));
        }
        String outputFileString = outputFilePath.toString();
        return new File(outputFileString);
    }

    private BufferedImage readImage(ImageInputStream imageInputStream, Iterator<ImageReader> readers, TotalReadPercentageDTO totalReadPercentageDTO) {
        try {
            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInputStream, true, true);
                ExtendedIIOReadProgressListener readerListener = getReaderListener(totalReadPercentageDTO);

                reader.addIIOReadProgressListener(readerListener);


                interruptIfNecessary();
                var data = reader.read(0);
                if (readerListener.getException() != null) {
                    throw readerListener.getException();
                }
                return data;
            } finally {
                reader.dispose();
            }

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void writeImageOuter(String newFileFormat, Consumer<Float> onProgress, File outputFile, TotalReadPercentageDTO totalReadPercentageDTO, BufferedImage image) throws Exception {
        try {
            writeImageInner(newFileFormat, onProgress, outputFile, totalReadPercentageDTO, image);
        } catch (Exception e) {
            deleteBrokenFileIfExists(outputFile);
            throw e;
        } finally {
            cancelTask();
        }
    }

    private void writeImageInner(String newFileFormat, Consumer<Float> onProgress, File outputFile, TotalReadPercentageDTO totalReadPercentageDTO, BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(newFileFormat);
        if (!writers.hasNext()) {
            log.debug("No writers found for format: {}", newFileFormat);
            throw new IllegalStateException("No writer found for format: " + newFileFormat);
        }

        ImageWriter writer = writers.next();
        try {
            ImageOutputStream currentOutputStream = ImageIO.createImageOutputStream(outputFile);
            writer.setOutput(currentOutputStream);
            ExtendedIIOWriteProgressListener writerListener = getWriterListener(onProgress, totalReadPercentageDTO);
            writer.addIIOWriteProgressListener(writerListener);


            if (Thread.currentThread().isInterrupted()) {
                log.error("Operation aborted 4");
                throw new ConversionManualAbortedException("Operation aborted");
            }

            writer.write(null, new IIOImage(image, null, null), null);

            if (writerListener.getException() != null) {
                throw writerListener.getException();
            }

            onProgress.accept(100f);
        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw new ConversionManualAbortedException("Conversion aborted by thread interruption.");
            }
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            deleteBrokenFileIfExists(outputFile);
            if (Thread.currentThread().isInterrupted()) {
                log.warn("Conversion failed, but thread was interrupted. Treating as cancelled.");
                throw new ConversionManualAbortedException("Conversion aborted by thread interruption.");
            }
            throw e;
        } finally {
            writer.dispose();
        }
    }

    private static void validateInputImage(Path sourceFile, String sourceFileFormat, List<String> validInputFormatListInLowerCase) {
        if (sourceFileFormat != null && !validInputFormatListInLowerCase.contains(sourceFileFormat.toLowerCase())) {
            String message = "invalid file format even if the extension is correct: " + sourceFile;
            log.warn(message);
            throw new InvalidFileFormatException(message);
        }
    }

    private interface ExtendedIIOWriteProgressListener extends IIOWriteProgressListener {
        Exception getException();
    }

    private static ExtendedIIOWriteProgressListener getWriterListener(Consumer<Float> onProgress, TotalReadPercentageDTO totalReadPercentageDTO) {
        return new ExtendedIIOWriteProgressListener() {
            private Exception exception;

            public Exception getException() {
                return exception;
            }

            @Override
            public void imageStarted(ImageWriter source, int imageIndex) {
                if (Thread.currentThread().isInterrupted()) {
                    log.error("Operation aborted 1");
                    exception = new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void imageProgress(ImageWriter source, float percentageDone) {
                if (Thread.currentThread().isInterrupted()) {
                    log.error("Operation aborted 2");
                    exception = new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                    return;
                }

                onProgress.accept(totalReadPercentageDTO.getTotalReadPercentage() / 2 + percentageDone / 2);
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
                this.exception = new RuntimeException("Write aborted 3");
            }
        };
    }

    private interface ExtendedIIOReadProgressListener extends IIOReadProgressListener {
        Exception getException();
    }

    private ExtendedIIOReadProgressListener getReaderListener(TotalReadPercentageDTO totalReadPercentageDTO) {
        return new ExtendedIIOReadProgressListener() {
            private Exception exception;

            public Exception getException() {
                return exception;
            }

            @Override
            public void sequenceStarted(ImageReader source, int minIndex) {
            }

            @Override
            public void sequenceComplete(ImageReader source) {
            }

            @Override
            public void imageStarted(ImageReader source, int imageIndex) {
                totalReadPercentageDTO.setTotalReadPercentage(1);
            }

            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                if (Thread.currentThread().isInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                    return;
                }
                totalReadPercentageDTO.setTotalReadPercentage((int) percentageDone);
            }

            @Override
            public void imageComplete(ImageReader source) {
                totalReadPercentageDTO.setTotalReadPercentage(100);
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
                this.exception = new RuntimeException("Read aborted");
            }
        };
    }

    private static void interruptIfNecessary() {
        if (Thread.currentThread().isInterrupted()) {
            log.error("Operation aborted 0");
            throw new ConversionManualAbortedException("Operation aborted");
        }
    }

    @Override
    public void cancelTask() {
        setCanceled(true);
        Thread.currentThread().interrupt();
    }


    @Builder
    @Getter
    @Setter
    protected static class TotalReadPercentageDTO {
        private int totalReadPercentage;
    }

}
