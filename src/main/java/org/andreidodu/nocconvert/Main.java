package org.andreidodu.nocconvert;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Application is starting");

        ImageIO.scanForPlugins();

        log.info("==========================[ START READERS ]=============================");
        String[] readers = ImageIO.getReaderFormatNames();
        log.info("{}", String.join(", ", readers));
        log.info("==========================[ END READERS ]=============================");

        log.info("==========================[ START WRITERS ]=============================");
        String[] writers = ImageIO.getWriterFormatNames();
        log.info("{}", String.join(", ", writers));
        log.info("==========================[ END WRITERS ]=============================");


        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        new GUIOrchestrator();
    }

}
