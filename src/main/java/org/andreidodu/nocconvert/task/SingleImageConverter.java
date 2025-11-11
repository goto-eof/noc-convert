package org.andreidodu.nocconvert.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.andreidodu.nocconvert.dto.ImageHeaderDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;
import org.andreidodu.nocconvert.enums.NotificationLevel;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.exception.InvalidFileFormatException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.andreidodu.nocconvert.helper.ImageUtil.*;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class SingleImageConverter {
    private static final Logger log = LogManager.getLogger(SingleImageConverter.class);
    public static final int VALIDATION_RETRY_TIMES = 5;
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final ExecutorService platformExecutorService;

    private final static Object IMAGE_IO_LOCK = new Object();
    @Setter
    @Getter
    private NotificationLevel notificationLevel = NotificationLevel.MEDIUM;
    private final static Object RENAME_LOCK = new Object();
    private ConvertImageInputDTO convertImageInputDTO;

    public SingleImageConverter(ExecutorService platformExecutorService) {
        this.platformExecutorService = platformExecutorService;
    }

    public void convertImage(ConvertImageInputDTO convertImageInputDTO) {
        if (canceled.get()) {
            throw new ConversionManualAbortedException();
        }
        this.convertImageInputDTO = convertImageInputDTO;

        log.debug("Converting image {} from {} to {}", convertImageInputDTO.sourceFile().getFileName(), convertImageInputDTO.sourceFile(), convertImageInputDTO.targetExtension());

        interruptIfNecessary();

        convertImageInputDTO.onStart().run();
        convertImageInputDTO.onProgress().accept(1f);

        Path sourceFile = convertImageInputDTO.sourceFile();

        Path tmpOutputFile = convertImageInputDTO.calculateTemporaryFilenameForMe().apply(convertImageInputDTO);

        try {
            ImageHeaderDTO sourceFileHeaders;
            synchronized (IMAGE_IO_LOCK) {
                sourceFileHeaders = getImageHeaders(sourceFile);
            }
            String sourceFileFormat = sourceFileHeaders.format();
            validateInputImage(convertImageInputDTO.sourceFile(), sourceFileFormat, convertImageInputDTO.readerFormatNames());

            if (sourceFileHeaders.format().equalsIgnoreCase(convertImageInputDTO.targetExtension())) {
                log.info("No need to convert image {} from {} to {}. I will just copy it.", convertImageInputDTO.sourceFile(), sourceFileHeaders.format(), convertImageInputDTO.targetExtension());

                if (tmpOutputFile == null) {
                    throw new RuntimeException("unable to copy the file");
                }
                // convertImageInputDTO.addToFileQueue().accept(tmpOutputFile);
                convertImageInputDTO.copyMe().accept(convertImageInputDTO);
                convertImageInputDTO.pass().run();
                return;
            }
        } catch (ConversionManualAbortedException e) {
            throw e;
        } catch (Exception e) {
            convertImageInputDTO.fail().accept(e);
            throw new RuntimeException(getRootCauseMessage(e), e);
        }

        BufferedImage image;
        TotalReadPercentageDTO totalReadPercentageDTO = TotalReadPercentageDTO.builder().build();

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(sourceFile.toFile())) {
            image = loadHealthyImage(convertImageInputDTO, imageInputStream, sourceFile, totalReadPercentageDTO);
        } catch (IOException e) {
            image = tryToLoadCorruptedImage(convertImageInputDTO, e, sourceFile, totalReadPercentageDTO, convertImageInputDTO.onProgress());
        }

        Objects.requireNonNull(image, "why here the image is null bro?!");
        try {
            interruptIfNecessary();

            image = safeDeepCopy(image);

            image = preprocessImage(convertImageInputDTO.sourceFile(), convertImageInputDTO.targetExtension(), image, platformExecutorService);

            interruptIfNecessary();

            writeImageOuter(convertImageInputDTO, tmpOutputFile, totalReadPercentageDTO, image, convertImageInputDTO.addToFileQueue());
            convertImageInputDTO.renameMe().accept(tmpOutputFile, convertImageInputDTO);
            convertImageInputDTO.pass().run();
        } catch (ConversionManualAbortedException e) {
            deleteCorruptedFile(tmpOutputFile);
            throw e;
        } catch (Throwable e) {
            deleteCorruptedFile(tmpOutputFile);
            convertImageInputDTO.fail().accept(new RuntimeException(getRootCauseMessage(e), e));
            if (e instanceof RejectedExecutionException || e instanceof InterruptedException) {
                throw new ConversionManualAbortedException("operation aborted");
            }
            throw new RuntimeException(getRootCauseMessage(e));
        }
    }

//    private static void deleteCorruptedFile(boolean fileRenamed, Path oldFilename, Path newFilename) {
//        if (fileRenamed) {
//            deleteCorruptedFile(newFilename);
//            return;
//        }
//        deleteCorruptedFile(oldFilename);
//    }

    private static void deleteCorruptedFile(Path tmpOutputFile) {
        if (!tmpOutputFile.toFile().exists()) {
            return;
        }
        try {
            FileUtils.forceDelete(tmpOutputFile.toFile());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Corrupt Data Handling -> sometimes it succeeds
     */
    private static BufferedImage tryToLoadCorruptedImage(ConvertImageInputDTO convertImageInputDTO, IOException e, Path sourceFile, TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> updateProgressFloatValue) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(sourceFile.toFile())) {
            return loadCorruptedImage(sourceFile, totalReadPercentageDTO, updateProgressFloatValue, imageInputStream, convertImageInputDTO);
        } catch (Exception eee) {
            log.error("Failed to restore the corrupted image: {}", getRootCauseMessage(eee), eee);
            convertImageInputDTO.fail().accept(new RuntimeException(getRootCauseMessage(e), e));
            throw new RuntimeException(getRootCauseMessage(e), e);
        }
    }

    private static BufferedImage loadCorruptedImage(Path sourceFile, TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> updateProgressFloatValue, ImageInputStream imageInputStream, ConvertImageInputDTO convertImageInputDTO) throws IOException {
        BufferedImage image;
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
        while (readers.hasNext()) {
            try {
                ImageReader reader = readers.next();
                reader.setInput(imageInputStream);
                ExtendedIIOReadProgressListener readerListener = getReaderListener(totalReadPercentageDTO, updateProgressFloatValue, convertImageInputDTO.notificationLevel());

                reader.addIIOReadProgressListener(readerListener);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                ImageTypeSpecifier imageType = reader.getRawImageType(0);
                image = imageType.createBufferedImage(width, height);
                ImageReadParam param = reader.getDefaultReadParam();
                param.setDestination(image);
                reader.read(0, param);
                reader.dispose();

                return image;
            } catch (IOException eee) {
                log.error("Invalid reader (corrupted phase). Trying the next one if exists...{}", getRootCauseMessage(eee));
            }
        }
        throw new RuntimeException("No ImageReader found for: " + sourceFile + " (corrupted phase)");
    }

    private BufferedImage loadHealthyImage(ConvertImageInputDTO convertImageInputDTO, ImageInputStream imageInputStream, Path sourceFile, TotalReadPercentageDTO totalReadPercentageDTO) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
        while (readers.hasNext()) {
            try {
                return readImage(imageInputStream, readers.next(), totalReadPercentageDTO, convertImageInputDTO.onProgress(), convertImageInputDTO);
            } catch (IOException eee) {
                log.error("Invalid reader (healthy phase). Trying the next one if exists...{}", getRootCauseMessage(eee));
            }
        }
        throw new RuntimeException("No ImageReader found for: " + sourceFile + " (healthy phase)");
    }

//    private static ImageReader getReader(ImageReader readers, ImageInputStream imageInputStream, Path sourceFile) {
//
//        validateReader(readers, sourceFile);
//        return readers.next();
//    }

    private static void validateReader(Iterator<ImageReader> readers, Path file) {
        if (!readers.hasNext()) {
            throw new RuntimeException("No ImageReader found for: " + file);
        }
    }

    private BufferedImage readImage(ImageInputStream imageInputStream, ImageReader reader, TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> updateProgressFloatValue, ConvertImageInputDTO convertImageInputDTO) throws IOException {
        try {
            try {
                reader.setInput(imageInputStream, true, true);
                ExtendedIIOReadProgressListener readerListener = getReaderListener(totalReadPercentageDTO, updateProgressFloatValue, convertImageInputDTO.notificationLevel());

                reader.addIIOReadProgressListener(readerListener);

                interruptIfNecessary();
                var data = reader.read(0);
                if (readerListener.getException() != null) {
                    throw readerListener.getException();
                }
                return data;
            } catch (ConversionManualAbortedException e) {
                log.debug("ConversionManualAbortedException", e);
                throw e;
            } catch (Exception e) {
                throw e;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void writeImageOuter(ConvertImageInputDTO convertImageInputDTO, Path tmpOutputFile, TotalReadPercentageDTO totalReadPercentageDTO, BufferedImage image, Consumer<Path> addToFileDeleteQueue) throws Exception {
        try {
            writeImageInner(convertImageInputDTO.targetExtension(), convertImageInputDTO.onProgress(), tmpOutputFile, totalReadPercentageDTO, image);
        } catch (ConversionManualAbortedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(getRootCauseMessage(e), e);
        }
    }

    private void validateFileCompletionWithRetry(Path outputFile) throws Exception {
        try {
            getImageHeaders(outputFile);
        } catch (Exception e) {
            throw new RuntimeException(getRootCauseMessage(e), e);
        }
    }

    private static void validationSleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error(getRootCauseMessage(e), e);
            throw new RuntimeException(e);
        }
    }

    private void writeImageInner(String newFileFormat, Consumer<Float> updateProgressFloatValue, Path tmpOutputFile, TotalReadPercentageDTO totalReadPercentageDTO, BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers;
        synchronized (IMAGE_IO_LOCK) {
            writers = ImageIO.getImageWritersByFormatName(newFileFormat);
        }
        if (!writers.hasNext()) {
            log.debug("No writers found for format: {}", newFileFormat);
            throw new IllegalStateException("No writer found for format: " + newFileFormat);
        }

        ImageWriter writer = writers.next();
        if (canceled.get() && !tmpOutputFile.toFile().exists()) {
            log.error("Operation aborted and file deleted");
            throw new ConversionManualAbortedException("Operation aborted");
        }
        try (ImageOutputStream currentOutputStream = ImageIO.createImageOutputStream(tmpOutputFile.toFile())) {
            writer.setOutput(currentOutputStream);
            ExtendedIIOWriteProgressListener writerListener = getWriterListener(updateProgressFloatValue);
            writer.addIIOWriteProgressListener(writerListener);

            if (Thread.currentThread().isInterrupted()) {
                log.error("Operation aborted 4");
                throw new ConversionManualAbortedException("Operation aborted");
            }
            try {

//                if (ICNS_FORMAT.equalsIgnoreCase(newFileFormat)) {
//                    var writeParam = writer.getDefaultWriteParam();
//                    if (writeParam != null) {
//                        java.awt.Rectangle sourceRegion = new java.awt.Rectangle(
//                                0,
//                                0,
//                                ICNS_MAX_SIZE,
//                                ICNS_MAX_SIZE
//                        );
//
//                        writeParam.setSourceRegion(sourceRegion);
//                    }
//                    writer.write(null, new IIOImage(image, null, null), writeParam);
//                }
//                if (ICNS_FORMAT.equalsIgnoreCase(newFileFormat)) {
//                    List<BufferedImage> icnsImages = new ArrayList<>();
//                    icnsImages.add(image);
//                    writer.prepareWriteSequence(null);
//                    writer.writeToSequence(new IIOImage(image, null, null), null);
//                    writer.endWriteSequence();
//                } else
                writer.write(null, new IIOImage(image, null, null), null);

            } catch (Exception e) {
                log.debug("unable to write: {}", convertImageInputDTO.sourceFile());
                throw new RuntimeException(getRootCauseMessage(e), e);
            }

            if (writerListener.getException() != null) {
                throw writerListener.getException();
            }
        } catch (ConversionManualAbortedException e) {
            throw new ConversionManualAbortedException("Conversion aborted by thread interruption.", e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
        if (sourceFileFormat == null || !validInputFormatListInLowerCase.contains(sourceFileFormat.toLowerCase())) {
            String message = "invalid file format even if the extension is correct: " + sourceFile;
            log.warn(message);
            throw new InvalidFileFormatException(message);
        }
    }

    private interface ExtendedIIOWriteProgressListener extends IIOWriteProgressListener {
        Exception getException();
    }

    private ExtendedIIOWriteProgressListener getWriterListener(Consumer<Float> updateProgressFloatValue) {
        return new ExtendedIIOWriteProgressListener() {
            private Exception exception;

            public Exception getException() {
                return exception;
            }

            @Override
            public void imageStarted(ImageWriter source, int imageIndex) {
                if (isInterrupted()) {
                    return;
                }
                updateProgressFloatValue.accept(1f);
            }

            private boolean isInterrupted() {
                if (Thread.currentThread().isInterrupted() && exception == null) {
                    log.error("Operation aborted 1");
                    exception = new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                    return true;
                }
                return false;
            }

            @Override
            public void imageProgress(ImageWriter source, float percentageDone) {
                if (isInterrupted() || notificationLevel == NotificationLevel.LOW) {
                    return;
                }
                updateProgressFloatValue.accept(50 + percentageDone / 2);
            }

            @Override
            public void imageComplete(ImageWriter source) {
                if (isInterrupted() || notificationLevel == NotificationLevel.LOW) {
                    return;
                }
                log.trace("Write image complete");
            }

            @Override
            public void thumbnailStarted(ImageWriter source, int imageIndex, int thumbnailIndex) {
                if (isInterrupted()) {
                    return;
                }
                log.trace("Write thumbnail started");
            }

            @Override
            public void thumbnailProgress(ImageWriter source, float percentageDone) {
                if (isInterrupted()) {
                    return;
                }
                log.trace("Write thumbnail progress");
            }

            @Override
            public void thumbnailComplete(ImageWriter source) {
                if (isInterrupted()) {
                    return;
                }
                log.trace("Write thumbnail complete");
            }

            @Override
            public void writeAborted(ImageWriter source) {
                if (isInterrupted()) {
                    return;
                }
                log.error("Operation aborted 3");
                this.exception = new RuntimeException("Write aborted 3");
            }
        };
    }

    private static void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public interface ExtendedIIOReadProgressListener extends IIOReadProgressListener {
        Exception getException();
    }

    private static ExtendedIIOReadProgressListener getReaderListener(TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> onProgress, NotificationLevel notificationLevel) {
        return new ExtendedIIOReadProgressListener() {
            private Exception exception;

            public Exception getException() {
                return exception;
            }

            @Override
            public void sequenceStarted(ImageReader source, int minIndex) {
                if (isInterrupted()) {
                }
            }

            @Override
            public void sequenceComplete(ImageReader source) {
                if (isInterrupted()) {
                }
            }

            @Override
            public void imageStarted(ImageReader source, int imageIndex) {
                if (isInterrupted() || notificationLevel == NotificationLevel.LOW) return;
                totalReadPercentageDTO.setTotalReadPercentage(1);
                onProgress.accept(1f);
            }

            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                if (isInterrupted() || notificationLevel == NotificationLevel.LOW) return;
                totalReadPercentageDTO.setTotalReadPercentage(percentageDone / 2);
                onProgress.accept(totalReadPercentageDTO.getTotalReadPercentage());

            }

            private boolean isInterrupted() {
                if (Thread.currentThread().isInterrupted() && exception == null) {
                    log.error("Operation aborted 5");
                    this.exception = new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                    return true;
                }
                return Thread.currentThread().isInterrupted();
            }

            @Override
            public void imageComplete(ImageReader source) {
                if (isInterrupted()) {
                }
            }

            @Override
            public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
                if (isInterrupted()) {
                }
            }

            @Override
            public void thumbnailProgress(ImageReader source, float percentageDone) {
                if (isInterrupted()) {
                }
            }

            @Override
            public void thumbnailComplete(ImageReader source) {
                if (isInterrupted()) {
                }
            }

            @Override
            public void readAborted(ImageReader source) {
                if (isInterrupted()) return;
                this.exception = new RuntimeException("Read aborted");
            }
        };
    }

    private void interruptIfNecessary() {
        if (Thread.currentThread().isInterrupted() || canceled.get()) {
            log.error("Operation aborted 0");
            throw new ConversionManualAbortedException("Operation aborted");
        }
    }

    public void cancel() {
        this.canceled.set(true);
    }


    @Builder
    @Getter
    @Setter
    protected static class TotalReadPercentageDTO {
        private float totalReadPercentage;
    }

}
