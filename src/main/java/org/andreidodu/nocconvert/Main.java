package org.andreidodu.nocconvert;

import org.andreidodu.nocconvert.config.ApplictionConfigurator;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.andreidodu.nocconvert.helper.ApplicationConfigLoggerUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Applying configuration...");
        ApplictionConfigurator.applyConfiguration();

        log.info("Printing image formats......");
        ApplicationConfigLoggerUtil.printAvailableReadersAndWriters();

        log.info("Starting GUI...");
        new GUIOrchestrator();
    }

}
