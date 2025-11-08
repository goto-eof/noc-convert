package org.andreidodu.nocconvert.helper;

import org.andreidodu.nocconvert.dto.ImageHeaderDTO;
import org.andreidodu.nocconvert.exception.ManualAbortedException;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.andreidodu.nocconvert.task.SingleImageConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ImageUtil {
    private static final Logger log = LogManager.getLogger(ImageUtil.class);
    public static final String BMP_FORMAT = "bmp";
    public static final String WBMP_FORMAT = "wbmp";
    private static final List<String> OPAQUE_FORMAT_LIST = List.of(BMP_FORMAT, WBMP_FORMAT, "jpg", "jpeg", "pbm", "pfm", "pgm", "pnm", "ppm");
    public static final String ICO_FORMAT = "ico";
    public static final String ICNS_FORMAT = "icns";
    public static final int ICO_MAX_SIZE = 256;
    public static final int ICNS_MAX_SIZE = 1024;

    public static BufferedImage normalizeAsIco(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage);
        return normalizeIconFormatCommon(ICO_FORMAT, TYPE_4BYTE_ABGR, bufferedImage, ICO_MAX_SIZE, false);
    }

    public static BufferedImage normalizeAsIcns(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage);
        return normalizeIconFormatCommon(ICNS_FORMAT, TYPE_INT_ARGB, bufferedImage, ICNS_MAX_SIZE, false);
    }

    public static BufferedImage convertToOpaque(BufferedImage originalImage, String newFormat) {
        Objects.requireNonNull(originalImage);

        BufferedImage newImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                WBMP_FORMAT.equalsIgnoreCase(newFormat) ? BufferedImage.TYPE_BYTE_BINARY : BufferedImage.TYPE_INT_RGB
        );

        java.awt.Graphics2D g2 = newImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(originalImage, 0, 0, Color.BLACK, null);
        g2.dispose();

        return newImage;
    }

    public static ImageHeaderDTO getImageHeaders(Path file) {
        Objects.requireNonNull(file);

        try (ImageInputStream iis = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("No reader found for " + file);
            }

            ImageReader reader = readers.next();
            SingleImageConverter.ExtendedIIOReadProgressListener readerListener = getReaderListener();
            reader.addIIOReadProgressListener(readerListener);

            reader.setInput(iis, true, true);

            if (readerListener.getException() != null) {
                throw readerListener.getException();
            }

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);

            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Invalid image size: " + width + "x" + height);
            }

            String format = reader.getFormatName();

            if (format == null || new ImageConverterUtil().getAvailableReadFormatList()
                    .stream()
                    .map(dtoFormat -> dtoFormat.getFormat().toLowerCase())
                    .noneMatch(formatFromList -> formatFromList.equalsIgnoreCase(format))) {
                throw new IllegalArgumentException("Invalid image format: " + format);
            }

            reader.dispose();

            return ImageHeaderDTO.builder()
                    .isHeaderFound(true)
                    .width(width)
                    .height(height)
                    .format(format)
                    .build();
        } catch (InterruptedException e) {
            throw new ManualAbortedException(getRootCauseMessage(e), e);
        } catch (Exception e) {
            throw new RuntimeException(getRootCauseMessage(e), e);
        }
    }

    private static SingleImageConverter.ExtendedIIOReadProgressListener getReaderListener() {
        return new SingleImageConverter.ExtendedIIOReadProgressListener() {
            private Exception exception;

            public Exception getException() {
                return exception;
            }

            @Override
            public void sequenceStarted(ImageReader source, int minIndex) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void sequenceComplete(ImageReader source) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void imageStarted(ImageReader source, int imageIndex) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void imageProgress(ImageReader source, float percentageDone) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }

            private boolean isInterrupted() {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                    return true;
                }
                return Thread.currentThread().isInterrupted();
            }

            private boolean isThreadInterrupted() {
                return Thread.currentThread().isInterrupted() && exception == null;
            }

            @Override
            public void imageComplete(ImageReader source) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void thumbnailProgress(ImageReader source, float percentageDone) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void thumbnailComplete(ImageReader source) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }

            @Override
            public void readAborted(ImageReader source) {
                if (isThreadInterrupted()) {
                    log.error("Operation aborted 5");
                    Thread.currentThread().interrupt();
                    this.exception = new InterruptedException("Conversion aborted by thread interruption.");
                }
            }
        };
    }


    private static BufferedImage normalizeIconFormatCommon(String format, int imageType, BufferedImage bufferedImage, int targetSize, boolean isOpaque) {
        int height = bufferedImage.getHeight();
        int width = bufferedImage.getWidth();

        if (ICO_FORMAT.equalsIgnoreCase(format) && width == height && width <= targetSize && bufferedImage.getType() == imageType) {
            return bufferedImage;
        }

        if (ICNS_FORMAT.equalsIgnoreCase(format) && width < ICNS_MAX_SIZE) {
            List<Integer> validSizes = List.of(1024, 512, 256, 128, 64, 32, 16);
            for (Integer size : validSizes) {
                if (width > size) {
                    targetSize = size;
                    break;
                }
            }
        }

        log.debug("target size: {}", targetSize);

        double scale = Math.min((double) targetSize / width, (double) targetSize / height);
        int scaledW = Math.max(16, (int) Math.round(width * scale));
        int scaledH = Math.max(16, (int) Math.round(height * scale));

        BufferedImage out = new BufferedImage(targetSize, targetSize, imageType);
        Graphics2D g2 = out.createGraphics();

        if (isOpaque) {
            g2.setColor(Color.black);
            g2.fillRect(0, 0, targetSize, targetSize);
        } else {
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, targetSize, targetSize);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        int x = (targetSize - scaledW) / 2;
        int y = (targetSize - scaledH) / 2;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(bufferedImage, x, y, x + scaledW, y + scaledH, 0, 0, width, height, null);
        g2.dispose();

        if (ICO_FORMAT.equalsIgnoreCase(format)) {
            swapRBGChannels(out);
        }
        return out;
    }

    private static void swapRBGChannels(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB &&
                image.getType() != BufferedImage.TYPE_INT_RGB &&
                image.getType() != TYPE_4BYTE_ABGR) {
            log.warn("Unsupported image type for R/B swap.");
            return;
        }

        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];

            int alpha = (pixel >> 24) & 0xff;
            int red = (pixel >> 16) & 0xff;
            int green = (pixel >> 8) & 0xff;
            int blue = (pixel) & 0xff;

            int newPixel = (alpha << 24) |
                    (blue << 16) |
                    (green << 8) |
                    (red);

            pixels[i] = newPixel;
        }

        image.setRGB(0, 0, w, h, pixels, 0, w);
    }

    public static java.util.List<FormatExtensionDTO> getFormatExtensionDTOList(String[] formatList) {
        Objects.requireNonNull(formatList);

        List<FormatExtensionDTO> imageFormatList = new ArrayList<>();
        String[] arrayString = Arrays.stream(formatList)
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .toArray(String[]::new);

        FormatExtensionDTO.reset();

        for (String format : arrayString) {
            imageFormatList.add(new FormatExtensionDTO(format, FormatExtensionMapper.getExtension(format)));
        }
        return imageFormatList;
    }

    private static boolean isNotCompatibleWithIcnsFormat(BufferedImage image) {
        Objects.requireNonNull(image);
        List<Integer> validSizes = List.of(1024, 512, 256, 128, 64, 32, 16).reversed();
        return !validSizes.contains(image.getWidth()) || !validSizes.contains(image.getHeight()) || image.getWidth() != image.getHeight();
    }

    public static BufferedImage preprocessImage(Path sourceFile, String newFileFormat, BufferedImage image, ExecutorService cpuPool) {
        Objects.requireNonNull(sourceFile);
        Objects.requireNonNull(newFileFormat);
        Objects.requireNonNull(cpuPool);
        Objects.requireNonNull(newFileFormat);

        try {
            return adaptImageForTheOutputFormat(sourceFile, newFileFormat, image, cpuPool);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static BufferedImage adaptImageForTheOutputFormat(Path sourceFile, String newFileFormat, BufferedImage image, ExecutorService cpuPool) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(sourceFile);
        Objects.requireNonNull(newFileFormat);
        Objects.requireNonNull(cpuPool);
        Objects.requireNonNull(image);

        if (isIsNotCompatibleWithIcoFormat(newFileFormat, image)) {
            return adaptForIcoFormat(sourceFile, image, cpuPool);
        }

        if (ICNS_FORMAT.equalsIgnoreCase(newFileFormat) && isNotCompatibleWithIcnsFormat(image)) {
            return adaptForIcnsFormat(sourceFile, image, cpuPool);
        }

        if (OPAQUE_FORMAT_LIST.contains(newFileFormat)) {
            return adaptForOpaqueFormat(image, newFileFormat, cpuPool);
        }

        return image;
    }

    private static boolean isIsNotCompatibleWithIcoFormat(String newFileFormat, BufferedImage image) {
        return ICO_FORMAT.equalsIgnoreCase(newFileFormat) && (image.getWidth() > ICO_MAX_SIZE || image.getHeight() > ICO_MAX_SIZE || image.getWidth() % 8 != 0) ||
                ICO_FORMAT.equalsIgnoreCase(newFileFormat) && image.getType() != TYPE_4BYTE_ABGR;
    }

    private static BufferedImage adaptForOpaqueFormat(BufferedImage image, String newFormat, ExecutorService cpuPool) throws InterruptedException, ExecutionException {
        final BufferedImage originalImage = image;
        return cpuPool.submit(() -> convertToOpaque(originalImage, newFormat)).get();
    }

    private static BufferedImage adaptForIcnsFormat(Path sourceFile, BufferedImage image, ExecutorService cpuPool) throws InterruptedException, ExecutionException {
        String message = String.format("the specific constraints for icns format are not respected by the input image, so I will force conversion of %s.", sourceFile);
        log.warn(message);


        final BufferedImage originalImage = image;
        image = cpuPool.submit(() -> normalizeAsIcns(originalImage)).get();

        message = String.format("new image size: %s x %s", image.getWidth(), image.getHeight());
        log.warn(message);
        return image;
    }

    private static BufferedImage adaptForIcoFormat(Path sourceFile, BufferedImage image, ExecutorService cpuPool) throws InterruptedException, ExecutionException {
        String message = String.format("the specific constraints for ico format are not respected by the input image, so I will force conversion of %s.", sourceFile);
        log.warn(message);

        final BufferedImage originalImage = image;
        image = cpuPool.submit(() -> normalizeAsIco(originalImage)).get();

        message = String.format("new image size: %s x %s", image.getWidth(), image.getHeight());
        log.warn(message);
        return image;
    }

    public static String renameFileExtension(String oldFileName, String newFileFormat) {
        Objects.requireNonNull(oldFileName);
        Objects.requireNonNull(newFileFormat);

        int lastIndexOf = oldFileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return oldFileName + "." + FormatExtensionMapper.getExtension(newFileFormat);
        }

        if (oldFileName.chars().asDoubleStream().filter(c -> c == '.').count() == 1) {
            return oldFileName + "." + FormatExtensionMapper.getExtension(newFileFormat);
        }

        return oldFileName.substring(0, lastIndexOf) + "." + FormatExtensionMapper.getExtension(newFileFormat);
    }

    /**
     * Necessary to avoid issues with a possibly corrupted ruster of the original image
     */
    public static BufferedImage safeDeepCopy(BufferedImage source) {
        if (source == null) {
            return null;
        }

        int targetType = BufferedImage.TYPE_INT_ARGB;

        if (source.getType() == targetType) {
            return source;
        }

        BufferedImage target = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                targetType
        );

        Graphics2D g2d = target.createGraphics();

        if (source.getTransparency() == java.awt.Transparency.OPAQUE) {
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, source.getWidth(), source.getHeight());
        }

        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();

        return target;
    }

    public static BufferedImage safeDeepCopy2(BufferedImage source) {
        if (source == null) {
            return null;
        }
        java.awt.image.ColorModel cm = source.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();

        java.awt.image.WritableRaster raster = source.copyData(null);

        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

}
