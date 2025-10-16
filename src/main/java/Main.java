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

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("NoCloud Bulk Image Converter");
            JPanel mainPanel = getMainPanel(frame);
            frame.setContentPane(mainPanel);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static JPanel getMainPanel(JFrame frame) {
        return new MainGUI(frame).getMainPanel();
    }

}
