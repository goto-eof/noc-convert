package org.andreidodu.nocconvert.helper;

import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;

import javax.imageio.ImageIO;
import java.util.List;

import static org.andreidodu.nocconvert.helper.ImageUtil.getFormatExtensionDTOList;

public class ImageConverterUtil {
    private final static Object LOCK_WRITERS = new Object();
    private final static Object LOCK_READERS = new Object();

    public List<FormatExtensionDTO> getAvailableWriteFormatList() {
        synchronized (LOCK_WRITERS) {
            return getFormatExtensionDTOList(ImageIO.getWriterFormatNames());
        }
    }

    public List<FormatExtensionDTO> getAvailableReadFormatList() {
        synchronized (LOCK_READERS) {
            return getFormatExtensionDTOList(ImageIO.getReaderFormatNames());
        }
    }

}
