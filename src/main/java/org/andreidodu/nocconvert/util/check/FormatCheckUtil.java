package org.andreidodu.nocconvert.util.check;

import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

public class FormatCheckUtil {
    private static final Logger log = LogManager.getLogger(FormatCheckUtil.class);

    public static List<FormatExtensionDTO> testAvailableFormatsAndGet(List<FormatExtensionDTO> imageFormatList) {
        BufferedImage bufferedImage = createTestImage();
        return imageFormatList.stream().filter(format -> isPassTest(bufferedImage, format.getFormat())).toList();
    }

    private static boolean isPassTest(BufferedImage image, String format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean canWrite = ImageIO.write(image, format, baos);
            if (!canWrite) {
                log.warn("format testing: unable to write in the {} format (invalid architecture?), so I will hide this feature", format);
                return false;
            }
            log.debug("format testing: format {} tested successfully", format);
            return canWrite;
        } catch (Throwable e) {
            log.debug("unable to write in the following format: {}, so I will hide this feature.", format);
            return false;
        }
    }

    private static BufferedImage createTestImage() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.GREEN.getRGB());
        return image;
    }
}
