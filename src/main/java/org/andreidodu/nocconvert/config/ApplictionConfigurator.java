package org.andreidodu.nocconvert.config;

import com.formdev.flatlaf.FlatDarculaLaf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;

public class ApplictionConfigurator {
    private static final Logger log = LogManager.getLogger(ApplictionConfigurator.class);

    public static void applyConfiguration(){
        applyImageIOConfiguration();


        applyLookAndFeel();
        // LEDTLogger.logEDTActivity("EDT", );
    }

    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    private static void applyImageIOConfiguration() {
        System.setProperty("com.sun.media.imageio.stream.usecache", "false");
        ImageIO.setUseCache(false);
        ImageIO.scanForPlugins();
    }
}
