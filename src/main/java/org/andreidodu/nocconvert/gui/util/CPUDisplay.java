package org.andreidodu.nocconvert.gui.util;

import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.gui.constants.Colors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static org.andreidodu.nocconvert.helper.CpuUtil.calculateCpuLoadPercentage;

public class CPUDisplay extends JLabel implements ActionListener {

    private final Timer textUpdateTimer;

    private final Timer autoRepaintTimer;

    public CPUDisplay() {
        super("CPU: --");
        setVisible(ApplicationConfig.DEV_MODE);
        setForeground(Color.YELLOW);
        setFont(getFont().deriveFont(Font.BOLD, 14f));
        setToolTipText("CPU");
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
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setVisible(ApplicationConfig.DEV_MODE);
        if (!ApplicationConfig.DEV_MODE) {
            return;
        }

        int percentage = calculateCpuLoadPercentage();

        if (percentage < 20) {
            setForeground(Color.GREEN);
        } else if (percentage < 30) {
            setForeground(Colors.LIME);
        } else if (percentage < 50) {
            setForeground(Color.ORANGE);
        } else if (percentage < 70) {
            setForeground(Color.RED);
        }

        setText(String.format("CPU: %s%%", percentage));
    }

    public void stopAutoRepaint() {
        if (autoRepaintTimer.isRunning()) {
            autoRepaintTimer.stop();
        }
    }
}