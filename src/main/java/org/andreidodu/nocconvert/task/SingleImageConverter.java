package org.andreidodu.nocconvert.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.andreidodu.nocconvert.dto.ImageHeaderDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.exception.InvalidFileFormatException;
import org.andreidodu.nocconvert.util.ImageUtil;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static org.andreidodu.nocconvert.util.ImageUtil.*;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class SingleImageConverter {
    private static final Logger log = LogManager.getLogger(SingleImageConverter.class);
    @Setter
    private volatile boolean canceled = false;
    private final ExecutorService platformExecutorService;

    private final static Object IMAGE_IO_LOCK = new Object();

    public SingleImageConverter(ExecutorService platformExecutorService) {
        this.platformExecutorService = platformExecutorService;
    }

    public void convertImage(ConvertImageInputDTO convertImageInputDTO) {
        if (canceled) {
            throw new ConversionManualAbortedException();
        }

        log.debug("Converting image {} from {} to {}", convertImageInputDTO.sourceFile().getFileName(), convertImageInputDTO.sourceFile(), convertImageInputDTO.targetExtension());

        interruptIfNecessary();

        convertImageInputDTO.onStart().run();
        convertImageInputDTO.onProgress().accept(1f);

        Path sourceFile = convertImageInputDTO.sourceFile();
        Path tmpOutputFile = calculateOutputFile(convertImageInputDTO.sourceFile(), convertImageInputDTO.tmpPath(), convertImageInputDTO.targetExtension(), true);

        try {

            List<String> validInputFormatListInLowerCase;
            ImageHeaderDTO sourceFileHeaders;
            synchronized (IMAGE_IO_LOCK) {
                sourceFileHeaders = getImageHeaders(sourceFile);
                validInputFormatListInLowerCase = Arrays.stream(ImageIO.getReaderFormatNames()).toList();
            }
            String sourceFileFormat = sourceFileHeaders.format();
            validateInputImage(convertImageInputDTO.sourceFile(), sourceFileFormat, validInputFormatListInLowerCase, convertImageInputDTO.fail());

            if (sourceFileHeaders.format().equalsIgnoreCase(convertImageInputDTO.targetExtension())) {
                log.info("No need to convert image {} from {} to {}. I will just copy it.", convertImageInputDTO.sourceFile(), sourceFileHeaders.format(), convertImageInputDTO.targetExtension());
                Path outputFile = calculateOutputFile(convertImageInputDTO.sourceFile(), convertImageInputDTO.destinationPath(), convertImageInputDTO.targetExtension(), false);
                Files.copy(sourceFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
                convertImageInputDTO.addToFileRenameQueue().accept(outputFile);
                convertImageInputDTO.pass().run();
                return;
            }
        } catch (ConversionManualAbortedException e) {
            throw e;
        } catch (Exception e) {
            convertImageInputDTO.fail().accept(e);
            throw new RuntimeException(getRootCauseMessage(e), e);
        }

        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(sourceFile.toFile())) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

            validateReader(readers, sourceFile);

            TotalReadPercentageDTO totalReadPercentageDTO = TotalReadPercentageDTO.builder().build();

            BufferedImage image = readImage(imageInputStream, readers, totalReadPercentageDTO, convertImageInputDTO.onProgress());

            interruptIfNecessary();

            image = preprocessImage(convertImageInputDTO.sourceFile(), convertImageInputDTO.targetExtension(), image, platformExecutorService);

            interruptIfNecessary();

            writeImageOuter(convertImageInputDTO, tmpOutputFile, totalReadPercentageDTO, image, convertImageInputDTO.addToFileRenameQueue());
        } catch (ConversionManualAbortedException e) {
            throw e;
        } catch (Exception e) {
            convertImageInputDTO.fail().accept(e);
            throw new RuntimeException(getRootCauseMessage(e), e);
        }
    }


    private static void validateReader(Iterator<ImageReader> readers, Path file) {
        if (!readers.hasNext()) {
            throw new RuntimeException("No ImageReader found for: " + file);
        }
    }

    private static Path calculateOutputFile(Path sourceFile, Path destinationFile, String newFileFormat, boolean isTemporary) {
        if (isTemporary) {
            newFileFormat = newFileFormat.toLowerCase() + ".tmp";
        }

        String fileName;
        Path outputFilePath;
        do {
            fileName = UUID.randomUUID() + "." + sourceFile.getFileName().toString();
            outputFilePath = destinationFile.resolve(renameFileExtension(fileName, newFileFormat));
        } while (outputFilePath.toFile().exists());

        String outputFileString = outputFilePath.toString();
        return Path.of(outputFileString);
    }

    private BufferedImage readImage(ImageInputStream imageInputStream, Iterator<ImageReader> readers, TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> updateProgressFloatValue) throws IOException {
        try {
            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInputStream, true, true);
                ExtendedIIOReadProgressListener readerListener = getReaderListener(totalReadPercentageDTO, updateProgressFloatValue);

                reader.addIIOReadProgressListener(readerListener);

                interruptIfNecessary();
                var data = reader.read(0);
                if (readerListener.getException() != null) {
                    throw readerListener.getException();
                }
                return data;
            } catch (Exception e) {
                throw e;
            } finally {
                reader.dispose();
            }

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void writeImageOuter(ConvertImageInputDTO convertImageInputDTO, Path tmpOutputFile, TotalReadPercentageDTO totalReadPercentageDTO, BufferedImage image, Consumer<Path> addToFileRenameQueue) throws Exception {
        try {
            writeImageInner(convertImageInputDTO.targetExtension(), convertImageInputDTO.onProgress(), tmpOutputFile, totalReadPercentageDTO, image);
            Path outputFile = calculateOutputFile(convertImageInputDTO.sourceFile(), convertImageInputDTO.destinationPath(), convertImageInputDTO.targetExtension(), false);
            validateFileCompletionWithRetry(tmpOutputFile, outputFile, convertImageInputDTO.pass(), convertImageInputDTO.fail(), addToFileRenameQueue);
        } catch (Throwable e) {
            throw new RuntimeException(getRootCauseMessage(e), e);
        }
    }

    private void validateFileCompletionWithRetry(Path tmpOutputFile, Path outputFile, Runnable pass, Consumer<Exception> fail, Consumer<Path> addToFileRenameQueue) {
        int i = 5;

        while (i > 0) {
            try {
                validateFileCompletion(tmpOutputFile, outputFile, pass);
                addToFileRenameQueue.accept(outputFile);
                return;
            } catch (Exception e) {
                log.debug(getRootCauseMessage(e), e);
                if (i == 1) {
                    moveFailedImageToTmp(tmpOutputFile, outputFile, fail, e);
                    return;
                }
            }
            validationSleep(fail);
            i--;
        }
    }

    private static void moveFailedImageToTmp(Path tmpOutputFile, Path outputFile, Consumer<Exception> fail, Exception e) {
        try {
            Files.move(outputFile, tmpOutputFile, StandardCopyOption.ATOMIC_MOVE);
            fail.accept(new RuntimeException(getRootCauseMessage(e), e));
        } catch (IOException ex) {
            fail.accept(new RuntimeException(getRootCauseMessage(e), ex));
            throw new RuntimeException(ex);
        }
    }

    private static void validateFileCompletion(Path tmpOutputFile, Path outputFile, Runnable pass) throws IOException {
        Files.move(tmpOutputFile, outputFile, StandardCopyOption.ATOMIC_MOVE);
        ImageUtil.getImageHeaders(outputFile);
        pass.run();
    }

    private static void validationSleep(Consumer<Exception> fail) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail.accept(new RuntimeException(getRootCauseMessage(e), e));
            log.debug(getRootCauseMessage(e), e);
            throw new RuntimeException(e);
        }
    }

    private void writeImageInner(String newFileFormat, Consumer<Float> updateProgressFloatValue, Path outputFile, TotalReadPercentageDTO totalReadPercentageDTO, BufferedImage image) throws Exception {
        Iterator<ImageWriter> writers;
        synchronized (IMAGE_IO_LOCK) {
            writers = ImageIO.getImageWritersByFormatName(newFileFormat);
        }
        if (!writers.hasNext()) {
            log.debug("No writers found for format: {}", newFileFormat);
            throw new IllegalStateException("No writer found for format: " + newFileFormat);
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream currentOutputStream = ImageIO.createImageOutputStream(outputFile.toFile())) {
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
                throw new RuntimeException(getRootCauseMessage(e), e);
            }

            if (writerListener.getException() != null) {
                throw writerListener.getException();
            }

        } catch (IOException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw new ConversionManualAbortedException("Conversion aborted by thread interruption.");
            }
            throw e;
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

    private static void validateInputImage(Path sourceFile, String sourceFileFormat, List<String> validInputFormatListInLowerCase, Consumer<Exception> fail) {
        if (sourceFileFormat != null && !validInputFormatListInLowerCase.contains(sourceFileFormat.toLowerCase())) {
            String message = "invalid file format even if the extension is correct: " + sourceFile;
            log.warn(message);
            InvalidFileFormatException e = new InvalidFileFormatException(message);
            fail.accept(e);
            throw e;
        }
    }

    private interface ExtendedIIOWriteProgressListener extends IIOWriteProgressListener {
        Exception getException();
    }

    private static ExtendedIIOWriteProgressListener getWriterListener(Consumer<Float> updateProgressFloatValue) {
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
                if (isInterrupted()) {
                    return;
                }
                updateProgressFloatValue.accept(50 + percentageDone / 2);
            }

            @Override
            public void imageComplete(ImageWriter source) {
                if (isInterrupted()) {
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

    private interface ExtendedIIOReadProgressListener extends IIOReadProgressListener {
        Exception getException();
    }

    private ExtendedIIOReadProgressListener getReaderListener(TotalReadPercentageDTO totalReadPercentageDTO, Consumer<Float> onProgress) {
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
                if (isInterrupted()) return;
                totalReadPercentageDTO.setTotalReadPercentage(1);
                onProgress.accept(1f);
            }

            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                if (isInterrupted()) return;
                totalReadPercentageDTO.setTotalReadPercentage(percentageDone / 2);
                onProgress.accept(totalReadPercentageDTO.getTotalReadPercentage());

            }

            private boolean isInterrupted() {
                if (Thread.currentThread().isInterrupted() && exception == null) {
                    log.error("Operation aborted 5");
                    this.exception = new ConversionManualAbortedException("Conversion aborted by thread interruption.");
                    return true;
                }
                return false;
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
        if (Thread.currentThread().isInterrupted() || canceled) {
            log.error("Operation aborted 0");
            throw new ConversionManualAbortedException("Operation aborted");
        }
    }

    public void interruptTask() {
        setCanceled(true);
    }


    @Builder
    @Getter
    @Setter
    protected static class TotalReadPercentageDTO {
        private float totalReadPercentage;
    }

}
