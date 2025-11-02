package org.andreidodu.nocconvert.util.check;

import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class FormatCheckUtil {
    private static final Logger log = LogManager.getLogger(FormatCheckUtil.class);

    public static List<FormatExtensionDTO> testAvailableFormatsAndGet(List<FormatExtensionDTO> imageFormatList) {
        BufferedImage bufferedImage = createTestImage(BufferedImage.TYPE_INT_RGB);
        BufferedImage bufferedImageIco = createTestImage(BufferedImage.TYPE_4BYTE_ABGR);
        return imageFormatList.stream()
                .filter(format -> isPassTest("ico".equalsIgnoreCase(format.getFormat()) ? bufferedImageIco : bufferedImage, format.getFormat()))
                .toList();
    }

    private static boolean isPassTest(BufferedImage image, String format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean canWrite = ImageIO.write(image, format, baos);
            if (!canWrite) {
                log.warn("format testing: unable to write in the {} format (invalid architecture?), so I will hide this feature", format);
                return false;
            }
            log.debug("format testing: format {} tested successfully", format);
            return true;
        } catch (IOException ioException) {
            log.debug("format testing: failed to test {} format. More info: {}", format, ioException.getMessage(), ioException);
            return true;
        } catch (Error e) {
            log.warn("unable to write in the following format: {}, so I will hide this feature. More info: {}", format, e.getMessage(), e);
            return false;
        }
    }

    private static BufferedImage createTestImage(int type) {
        BufferedImage image = new BufferedImage(1, 1, type);
        image.setRGB(0, 0, Color.GREEN.getRGB());
        return image;
    }
}
