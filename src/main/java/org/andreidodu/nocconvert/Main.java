package org.andreidodu.nocconvert;

import org.andreidodu.nocconvert.gui.GUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Application is starting");

//        IIORegistry registry = IIORegistry.getDefaultInstance();
//        registry.registerServiceProvider(new PDFImageReaderSpi());
//        registry.registerServiceProvider(new PDFImageWriterSpi());

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
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarculaLaf());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("NoCloud Bulk Image Converter");
            JPanel mainPanel = getMainPanel(frame);
            frame.setContentPane(mainPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.revalidate();
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        deleteLogFileOnExit();
    }

    private static void deleteLogFileOnExit() {
        File logFile = new File("logs/application.log");
        logFile.deleteOnExit();
    }

    private static JPanel getMainPanel(JFrame frame) {
        return new GUI(frame).getMainPanel();
    }

}
