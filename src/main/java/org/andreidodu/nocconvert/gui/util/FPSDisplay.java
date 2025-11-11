package org.andreidodu.nocconvert.gui.util;

import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.gui.constants.Colors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FPSDisplay extends JLabel implements ActionListener {

    private long lastTime = System.nanoTime();
    private int frames = 0;

    private final Timer textUpdateTimer;

    private final Timer autoRepaintTimer;

    public FPSDisplay() {
        super("FPS: --");
        setVisible(ApplicationConfig.DEV_MODE);
        setForeground(Color.RED);
        setFont(getFont().deriveFont(Font.BOLD, 16f));
        setToolTipText("FPS");
        setBackground(Color.BLACK);
        setOpaque(true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 2, 2, 2, Colors.LIME),
                new EmptyBorder(8, 8, 8, 8)
        ));


        textUpdateTimer = new Timer(1000, this);
        textUpdateTimer.start();

        autoRepaintTimer = new Timer(16, e -> {
            this.repaint();
        });
        autoRepaintTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        setVisible(ApplicationConfig.DEV_MODE);
        if (!ApplicationConfig.DEV_MODE) {
            return;
        }
        super.paintComponent(g);
        frames++;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setVisible(ApplicationConfig.DEV_MODE);
        if (!ApplicationConfig.DEV_MODE) {
            return;
        }
        long currentTime = System.nanoTime();
        double intervalSeconds = (currentTime - lastTime) / 1_000_000_000.0;
        double fps = frames / intervalSeconds;

        int displayFps = (int) Math.round(fps);

        if (displayFps < 20) {
            setForeground(Color.RED);
        } else if (displayFps < 30) {
            setForeground(Color.ORANGE);
        } else if (displayFps < 50) {
            setForeground(Colors.LIME);
        } else if (displayFps < 70) {
            setForeground(Color.GREEN);
        }

        setText(String.format("FPS: %d", displayFps));

        frames = 0;
        lastTime = currentTime;
    }

    public void stopAutoRepaint() {
        if (autoRepaintTimer.isRunning()) {
            autoRepaintTimer.stop();
        }
    }
}