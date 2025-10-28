package org.andreidodu.nocconvert.service.impl;

import lombok.Setter;
import org.andreidodu.nocconvert.dto.ImageHeaderDTO;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.exception.InvalidFileFormatException;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
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
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.andreidodu.nocconvert.util.FileUtil.deleteBrokenFileIfExists;
import static org.andreidodu.nocconvert.util.ImageUtil.*;

public class ImageConverterServiceImpl implements ImageConverterService {
    private static final Logger log = LogManager.getLogger(ImageConverterServiceImpl.class);
    public static final String WEBP_FORMAT = "webp";

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
    public void convertImage(Path sourceFile, Path destinationPath, String newFileFormat, Runnable onStart, Consumer<Float> onProgress, Runnable onDone, Consumer<String> writeAborted) throws IOException {
        if (canceled) {
            return;
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

        imageInputStream = ImageIO.createImageInputStream(file);

        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

        validateReader(readers, file);

        int[] readPercentage = new int[1];
        readPercentage[0] = 0;

        BufferedImage image = readImage(readers, readPercentage);

        interruptIfNecessary();

        image = preprocessImage(sourceFile, newFileFormat, image, CPU_POOL);

        interruptIfNecessary();

        File outputFile = calculateOutputFile(sourceFile, destinationPath, newFileFormat);

        writeImageOuter(newFileFormat, onProgress, onDone, outputFile, readPercentage, image);

    }

    private static void validateReader(Iterator<ImageReader> readers, File file) throws IOException {
        if (!readers.hasNext()) {
            throw new IOException("No ImageReader found for: " + file);
        }
    }

    private static File calculateOutputFile(Path sourceFile, Path destinationPath, String newFileFormat) {
        String fileName = sourceFile.getFileName().toString();
        Path outputFilePath = destinationPath.resolve(renameFileExtension(fileName, newFileFormat));
        String outputFileString = outputFilePath.toString();
        File outputFile = new File(outputFileString);
        return outputFile;
    }

    private BufferedImage readImage(Iterator<ImageReader> readers, int[] readPercentage) throws IOException {
        ImageReader reader = readers.next();
        reader.setInput(imageInputStream, true, true);
        reader.addIIOReadProgressListener(getReaderListener(readPercentage));

        interruptIfNecessary();
        BufferedImage image = reader.read(0);
        return image;
    }

    private void writeImageOuter(String newFileFormat, Consumer<Float> onProgress, Runnable onDone, File outputFile, int[] readPercentage, BufferedImage image) {
        try {
            writeImageInner(newFileFormat, onProgress, onDone, outputFile, readPercentage, image);
        } catch (ConversionManualAbortedException e) {
            deleteBrokenFileIfExists(outputFile);
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            deleteBrokenFileIfExists(outputFile);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (writer != null) {
                writer.dispose();
            }
            closeAllStreams();
            if (WEBP_FORMAT.equalsIgnoreCase(newFileFormat)) {
                onDone.run();
            }
        }
    }

    private void writeImageInner(String newFileFormat, Consumer<Float> onProgress, Runnable onDone, File outputFile, int[] readPercentage, BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(newFileFormat);
        if (!writers.hasNext()) {
            log.debug("No writers found for format: {}", newFileFormat);
            throw new IllegalStateException("No writer found for format: " + newFileFormat);
        }

        writer = writers.next();
        try {

            currentOutputStream = ImageIO.createImageOutputStream(outputFile);
            writer.setOutput(currentOutputStream);
            writer.addIIOWriteProgressListener(getWriterListener(onProgress, readPercentage, onDone));

            if (Thread.currentThread().isInterrupted()) {
                log.error("Operation aborted 4");
                throw new ConversionManualAbortedException("Operation aborted");
            }

            writer.write(null, new IIOImage(image, null, null), null);
            onProgress.accept(100f);

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
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static void validateInputImage(Path sourceFile, String sourceFileFormat, List<String> validInputFormatListInLowerCase) {
        if (sourceFileFormat != null && !validInputFormatListInLowerCase.contains(sourceFileFormat.toLowerCase())) {
            String message = "invalid file format even if the extension is correct: " + sourceFile;
            log.warn(message);
            throw new InvalidFileFormatException(message);
        }
    }

    private static IIOWriteProgressListener getWriterListener(Consumer<Float> onProgress, int[] readPercentage, Runnable onDone) {
        return new IIOWriteProgressListener() {
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
                    throw new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                }

                onProgress.accept(readPercentage[0] / 2 + percentageDone / 2);
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
                log.error("Operation aborted 3");
                onDone.run();
            }
        };
    }

    private IIOReadProgressListener getReaderListener(int[] readPercentage) {
        return new IIOReadProgressListener() {
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
                if (Thread.currentThread().isInterrupted()) {
                    log.error("Operation aborted 5");
                    throw new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                }

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
        };
    }

    private static void interruptIfNecessary() {
        if (Thread.currentThread().isInterrupted()) {
            log.error("Operation aborted 0");
            throw new ConversionManualAbortedException("Operation aborted");
        }
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

    @Override
    public List<FormatExtensionDTO> getAvailableWriteFormatList() {
        return getFormatExtensionDTOList(ImageIO.getWriterFormatNames());
    }

    @Override
    public List<FormatExtensionDTO> getAvailableReadFormatList() {
        return getFormatExtensionDTOList(ImageIO.getReaderFormatNames());
    }

}
