package org.andreidodu.nocconvert;

import org.andreidodu.nocconvert.gui.GUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Application is starting");
        ImageIO.scanForPlugins();

        String[] writers = ImageIO.getWriterFormatNames();
        for (String format : writers) {
            log.info("Available write format: {}", format);
        }

        String[] readers = ImageIO.getReaderFormatNames();
        for (String format : readers) {
            log.info("Available read format: {}", format);
        }


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
    }

    private static JPanel getMainPanel(JFrame frame) {
        return new GUI(frame).getMainPanel();
    }

}
