package org.andreidodu.nocconvert.gui.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FPSDisplay extends JLabel implements ActionListener {

    private long lastTime = System.nanoTime();
    private int frames = 0;

    private Timer textUpdateTimer;

    private Timer autoRepaintTimer;

    private final boolean active = false;

    public FPSDisplay() {
        super("FPS: --");
        if (!active) {
            setVisible(false);
            return;
        }
        setForeground(Color.RED);
        setFont(getFont().deriveFont(Font.BOLD, 12f));
        setToolTipText("FPS");

        textUpdateTimer = new Timer(1000, this);
        textUpdateTimer.start();

        autoRepaintTimer = new Timer(16, e -> {
            this.repaint();
        });
        autoRepaintTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!active) {
            return;
        }
        frames++;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!active) {
            return;
        }
        long currentTime = System.nanoTime();
        double intervalSeconds = (currentTime - lastTime) / 1_000_000_000.0;
        double fps = frames / intervalSeconds;

        int displayFps = (int) Math.round(fps);

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