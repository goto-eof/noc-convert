package org.andreidodu.nocconvert.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;

public class ApplicationConfigLoggerUtil {
    private static final Logger log = LogManager.getLogger(ApplicationConfigLoggerUtil.class);

    public static void printAvailableReadersAndWriters() {
        log.debug("==========================[ START READERS ]=============================");
        String[] readers = ImageIO.getReaderFormatNames();
        log.debug("{}", String.join(", ", readers));
        log.debug("==========================[ END READERS ]=============================");

        log.debug("==========================[ START WRITERS ]=============================");
        String[] writers = ImageIO.getWriterFormatNames();
        log.debug("{}", String.join(", ", writers));
        log.debug("==========================[ END WRITERS ]=============================");
    }
}
