package org.andreidodu.nocconvert.util;

import org.andreidodu.nocconvert.dto.ImageHeaderDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.mapper.FormatExtensionMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static java.awt.image.BufferedImage.TYPE_4BYTE_ABGR;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

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
        return normalizeIconFormatCommon(TYPE_4BYTE_ABGR, bufferedImage, ICO_MAX_SIZE, true);
    }

    public static BufferedImage normalizeAsIcns(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage);
        return normalizeIconFormatCommon(TYPE_INT_ARGB, bufferedImage, ICNS_MAX_SIZE, false);
    }

    public static BufferedImage convertToOpaque(BufferedImage originalImage, String newFormat) {
        Objects.requireNonNull(originalImage);

        BufferedImage newImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                WBMP_FORMAT.equalsIgnoreCase(newFormat) ? BufferedImage.TYPE_BYTE_BINARY : BufferedImage.TYPE_INT_RGB
        );

//        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        java.awt.Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, Color.BLACK, null);
        g2d.dispose();

        return newImage;
    }

    public static ImageHeaderDTO getImageHeaders(File file) {
        Objects.requireNonNull(file);

        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return ImageHeaderDTO.builder()
                        .isHeaderFound(false)
                        .build();
            }

            ImageReader reader = readers.next();
            reader.setInput(iis, true, true);

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            String format = reader.getFormatName();
            reader.dispose();

            return ImageHeaderDTO.builder()
                    .isHeaderFound(true)
                    .width(width)
                    .height(height)
                    .format(format)
                    .build();
        } catch (IOException e) {
            return ImageHeaderDTO.builder()
                    .isHeaderFound(false)
                    .build();
        }
    }

    private static BufferedImage normalizeIconFormatCommon(int imageType, BufferedImage bufferedImage, int targetSize, boolean isOpaque) {

        if (bufferedImage.getWidth() == targetSize && bufferedImage.getHeight() == targetSize && bufferedImage.getType() == imageType) {
            BufferedImage copy = new BufferedImage(targetSize, targetSize, imageType);
            Graphics2D g = copy.createGraphics();
            g.drawImage(bufferedImage, 0, 0, null);
            g.dispose();
            return copy;
        }

        double scale = Math.min((double) targetSize / bufferedImage.getWidth(), (double) targetSize / bufferedImage.getHeight());
        int scaledW = Math.max(1, (int) Math.round(bufferedImage.getWidth() * scale));
        int scaledH = Math.max(1, (int) Math.round(bufferedImage.getHeight() * scale));

        BufferedImage out = new BufferedImage(targetSize, targetSize, imageType);
        Graphics2D g2 = out.createGraphics();

        if (isOpaque) {
            g2.setColor(Color.black);
            g2.fillRect(0, 0, targetSize, targetSize);
        }

        if (!isOpaque) {
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, targetSize, targetSize);
            g2.setComposite(AlphaComposite.SrcOver);
        }

        int x = (targetSize - scaledW) / 2;
        int y = (targetSize - scaledH) / 2;

        g2.drawImage(bufferedImage, x, y, x + scaledW, y + scaledH, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
        g2.dispose();

        return out;
    }

    public static java.util.List<FormatExtensionDTO> getFormatExtensionDTOList(String[] formatList) {
        Objects.requireNonNull(formatList);

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

    private static boolean isNotCompatibleWithIcnsFormat(BufferedImage image) {
        Objects.requireNonNull(image);

        return image.getWidth() != image.getHeight() || image.getWidth() > ICNS_MAX_SIZE;
    }

    public static BufferedImage preprocessImage(Path sourceFile, String newFileFormat, BufferedImage image, ExecutorService cpuPool) {
        Objects.requireNonNull(sourceFile);
        Objects.requireNonNull(newFileFormat);
        Objects.requireNonNull(cpuPool);
        Objects.requireNonNull(newFileFormat);

        try {
            return adaptImageForTheOutputFormat(sourceFile, newFileFormat, image, cpuPool);
        } catch (InterruptedException | ExecutionException e) {
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
        return oldFileName.substring(0, lastIndexOf) + "." + FormatExtensionMapper.getExtension(newFileFormat);
    }

}
