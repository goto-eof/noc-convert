package org.andreidodu.nocconvert.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ImageHeaderDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;
import org.andreidodu.nocconvert.enums.NotificationLevel;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.exception.InvalidFileFormatException;
import org.andreidodu.nocconvert.exception.ManualAbortedException;
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
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.andreidodu.nocconvert.helper.ImageUtil.*;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class SingleImageConverterComponent implements CancellableConverterComponent {
    private static final Logger log = LogManager.getLogger(SingleImageConverterComponent.class);
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final ExecutorService platformExecutorService;

    private final static Object IMAGE_IO_LOCK = new Object();
    @Setter
    @Getter
    private NotificationLevel notificationLevel = NotificationLevel.MEDIUM;
    private ConvertImageInputDTO convertImageInputDTO;

    @Override
    public boolean isCancelled() {
        return canceled.get();
    }

    public SingleImageConverterComponent(ExecutorService platformExecutorService) {
        this.platformExecutorService = platformExecutorService;
    }

    public void convertImage(ConvertImageInputDTO convertImageInputDTO, ConversionItemDTO conversionItemDTO) {
        if (canceled.get() || Thread.currentThread().isInterrupted()) {
            throw new ConversionManualAbortedException("conversion aborted a1");
        }
        this.convertImageInputDTO = convertImageInputDTO;

        log.debug("Converting image {} from {} to {}", convertImageInputDTO.sourceFile().getFileName(), convertImageInputDTO.sourceFile(), convertImageInputDTO.targetExtension());

        if (Thread.currentThread().isInterrupted() || canceled.get()) {
            throw new ConversionManualAbortedException("operation aborted");
        }

        convertImageInputDTO.onStart().run();
        convertImageInputDTO.onProgress().accept(1f);

        Path sourceFile = convertImageInputDTO.sourceFile();

        Path tmpOutputFile = convertImageInputDTO.calculateTemporaryFilenameForMe().apply(conversionItemDTO);
        conversionItemDTO.setTmpFile(tmpOutputFile);

        try {
            ImageHeaderDTO sourceFileHeaders;
            interruptIfNecessary();
            synchronized (IMAGE_IO_LOCK) {
                interruptIfNecessary();
                sourceFileHeaders = getImageHeaders(sourceFile, this);
            }
            String sourceFileFormat = sourceFileHeaders.format();
            validateInputImage(convertImageInputDTO.sourceFile(), sourceFileFormat, convertImageInputDTO.readerFormatNames());

            if (sourceFileHeaders.format().equalsIgnoreCase(convertImageInputDTO.targetExtension())) {
                log.info("No need to convert image {} from {} to {}. I will just copy it.", convertImageInputDTO.sourceFile(), sourceFileHeaders.format(), convertImageInputDTO.targetExtension());

                if (tmpOutputFile == null) {
                    throw new RuntimeException("unable to copy the file");
                }
                convertImageInputDTO.copyMe().accept(conversionItemDTO);
                //convertImageInputDTO.addToFileQueue().accept(tmpOutputFile); -> NOTE: commented because: copy operation calls automatically addToFileQueue
                convertImageInputDTO.pass().accept(conversionItemDTO);
                return;
            }
        } catch (ConversionManualAbortedException e) {
            throw e;
        } catch (Exception e) {
            convertImageInputDTO.fail().accept(conversionItemDTO, e);
            throw new RuntimeException(getRootCauseMessage(e), e);
        }

        BufferedImage image;
        TotalReadPercentageDTO totalReadPercentageDTO = TotalReadPercentageDTO.builder().build();

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(sourceFile.toFile())) {
            if (Thread.currentThread().isInterrupted() || canceled.get()) {
                throw new ConversionManualAbortedException("operation aborted");
            }
            image = loadHealthyImage(convertImageInputDTO, imageInputStream, sourceFile, totalReadPercentageDTO);
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted() || canceled.get()) {
                throw new ConversionManualAbortedException("operation aborted");
            }
            image = tryToLoadCorruptedImage(convertImageInputDTO, e, sourceFile, totalReadPercentageDTO, convertImageInputDTO.onProgress());
        }

        Objects.requireNonNull(image, "why here the image is null bro?!");

        try {
            if (Thread.currentThread().isInterrupted() || canceled.get()) {
                throw new ConversionManualAbortedException("operation aborted");
            }

            image = safeDeepCopy(image);

            if (Thread.currentThread().isInterrupted() || canceled.get()) {
                throw new ConversionManualAbortedException("operation aborted");
            }

            image = preprocessImage(convertImageInputDTO.sourceFile(), convertImageInputDTO.targetExtension(), image, platformExecutorService);

            if (Thread.currentThread().isInterrupted() || canceled.get()) {
                throw new ConversionManualAbortedException("operation aborted");
            }

            writeImageOuter(convertImageInputDTO, tmpOutputFile, totalReadPercentageDTO, image);

            if (Thread.currentThread().isInterrupted() || canceled.get()) {
                throw new ConversionManualAbortedException("operation aborted");
            }

            //convertImageInputDTO.renameMe().accept(tmpOutputFile, convertImageInputDTO);
            // convertImageInputDTO.addToFileQueue().accept(tmpOutputFile);

//            if (Thread.currentThread().isInterrupted() || canceled.get()) {
//                throw new ConversionManualAbortedException("operation aborted");
//            }

            convertImageInputDTO.pass().accept(conversionItemDTO);
        } catch (ConversionManualAbortedException e) {
            if (Thread.currentThread().isInterrupted() || canceled.get()) {
                throw e;
            }
            deleteCorruptedFile(tmpOutputFile);
            throw e;
        } catch (Throwable e) {
            if (Thread.currentThread().isInterrupted() || canceled.get()) {
                throw new ConversionManualAbortedException("operation aborted");
            }
            deleteCorruptedFile(tmpOutputFile);
            convertImageInputDTO.fail().accept(conversionItemDTO, new RuntimeException(getRootCauseMessage(e), e));
            if (e instanceof RejectedExecutionException || e instanceof InterruptedException) {
                throw new ConversionManualAbortedException("operation aborted");
            }
            throw new RuntimeException(getRootCauseMessage(e));
        }
    }

    private void deleteCorruptedFile(Path tmpOutputFile) {
        if (!tmpOutputFile.toFile().exists() || canceled.get() || Thread.currentThread().isInterrupted()) {
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
    private BufferedImage tryToLoadCorruptedImage(ConvertImageInputDTO convertImageInputDTO, Exception e, Path sourceFile, TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> updateProgressFloatValue) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(sourceFile.toFile())) {
            return loadCorruptedImage(sourceFile, totalReadPercentageDTO, updateProgressFloatValue, imageInputStream, convertImageInputDTO);
        } catch (Exception eee) {
            log.error("Failed to restore the corrupted image: {}", getRootCauseMessage(eee), eee);
            // convertImageInputDTO.fail().accept(tmpOutputFile, new RuntimeException(getRootCauseMessage(e), e));
            throw new RuntimeException(getRootCauseMessage(e), e);
        }
    }

    private BufferedImage loadCorruptedImage(Path sourceFile, TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> updateProgressFloatValue, ImageInputStream imageInputStream, ConvertImageInputDTO convertImageInputDTO) throws Exception {
        BufferedImage image;
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
        while (readers.hasNext()) {
            interruptIfNecessary();
            ImageReader reader = readers.next();
            try {
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
                if (readerListener.getException() != null) {
                    reader.dispose();
                    throw readerListener.getException();
                }
                return image;
            } catch (IOException eee) {
                log.error("Invalid reader (corrupted phase). Trying the next one if exists...{}", getRootCauseMessage(eee));
            } catch (Exception e) {
                if (e instanceof ManualAbortedException) {
                    throw e;
                }
                throw new RuntimeException(getRootCauseMessage(e));
            } finally {
                reader.dispose();
            }
        }
        throw new RuntimeException("No ImageReader found for: " + sourceFile + " (corrupted phase)");
    }

    private BufferedImage loadHealthyImage(ConvertImageInputDTO convertImageInputDTO, ImageInputStream imageInputStream, Path sourceFile, TotalReadPercentageDTO totalReadPercentageDTO) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
        while (readers.hasNext()) {
            interruptIfNecessary();
            try {
                return readImage(imageInputStream, readers.next(), totalReadPercentageDTO, convertImageInputDTO.onProgress(), convertImageInputDTO);
            } catch (IOException eee) {
                log.error("Invalid reader (healthy phase). Trying the next one if exists...{}", getRootCauseMessage(eee));
            }
        }
        throw new RuntimeException("No ImageReader found for: " + sourceFile + " (healthy phase)");
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

    private void writeImageOuter(ConvertImageInputDTO convertImageInputDTO, Path tmpOutputFile, TotalReadPercentageDTO totalReadPercentageDTO, BufferedImage image) throws Exception {
        try {
            writeImageInner(convertImageInputDTO.targetExtension(), convertImageInputDTO.onProgress(), tmpOutputFile, totalReadPercentageDTO, image);
        } catch (ConversionManualAbortedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(getRootCauseMessage(e), e);
        }
    }

    private void writeImageInner(String newFileFormat, Consumer<Float> updateProgressFloatValue, Path tmpOutputFile, TotalReadPercentageDTO totalReadPercentageDTO, BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers;
        interruptIfNecessary();
        synchronized (IMAGE_IO_LOCK) {
            interruptIfNecessary();
            writers = ImageIO.getImageWritersByFormatName(newFileFormat);
        }
        if (!writers.hasNext()) {
            log.debug("No writers found for format: {}", newFileFormat);
            throw new IllegalStateException("No writer found for format: " + newFileFormat);
        }

        ImageWriter writer = writers.next();
        if (canceled.get() && !tmpOutputFile.toFile().exists()) {
            log.error("Operation aborted");
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

    private ExtendedIIOReadProgressListener getReaderListener(TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> onProgress, NotificationLevel notificationLevel) {
        return new ExtendedIIOReadProgressListener() {
            private Exception exception;

            public Exception getException() {
                return exception;
            }

            @Override
            public void sequenceStarted(ImageReader source, int minIndex) {
                if (isInterrupted()) {
                    throw new ManualAbortedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void sequenceComplete(ImageReader source) {
                if (isInterrupted()) {
                    throw new ManualAbortedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void imageStarted(ImageReader source, int imageIndex) {
                if (isInterrupted()) {
                    throw new ManualAbortedException("Conversion aborted by thread interruption.");
                }
                if (notificationLevel == NotificationLevel.LOW) return;
                totalReadPercentageDTO.setTotalReadPercentage(1);
                onProgress.accept(1f);
            }

            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                if (isInterrupted()) {
                    throw new ManualAbortedException("Conversion aborted by thread interruption.");
                }
                if (notificationLevel == NotificationLevel.LOW) return;
                totalReadPercentageDTO.setTotalReadPercentage(percentageDone / 2);
                onProgress.accept(totalReadPercentageDTO.getTotalReadPercentage());

            }

            private boolean isInterrupted() {
                if (canceled.get() && exception == null || Thread.currentThread().isInterrupted() && exception == null) {
                    log.error("Operation aborted 5");
                    this.exception = new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                    return true;
                }
                return Thread.currentThread().isInterrupted();
            }

            @Override
            public void imageComplete(ImageReader source) {
                if (isInterrupted()) {
                    throw new ManualAbortedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
                if (isInterrupted()) {
                    throw new ManualAbortedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void thumbnailProgress(ImageReader source, float percentageDone) {
                if (isInterrupted()) {
                    throw new ManualAbortedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void thumbnailComplete(ImageReader source) {
                if (isInterrupted()) {
                    throw new ManualAbortedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void readAborted(ImageReader source) {
                if (isInterrupted()) return;
                this.exception = new ManualAbortedException("Read aborted");
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
