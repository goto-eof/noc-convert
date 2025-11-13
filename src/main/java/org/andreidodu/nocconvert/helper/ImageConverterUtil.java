package org.andreidodu.nocconvert.helper;

import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;

import javax.imageio.ImageIO;
import java.util.List;

import static org.andreidodu.nocconvert.helper.ImageUtil.getFormatExtensionDTOList;

public class ImageConverterUtil {

    public List<FormatExtensionDTO> getAvailableWriteFormatList() {
        return getFormatExtensionDTOList(ImageIO.getWriterFormatNames());
    }

    public List<FormatExtensionDTO> getAvailableReadFormatList() {
        return getFormatExtensionDTOList(ImageIO.getReaderFormatNames());
    }

}
