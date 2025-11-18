package org.andreidodu.nocconvert.gui.util;

import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.gui.constants.Colors;
import org.andreidodu.nocconvert.helper.NumberUtil;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.DecimalFormat;

public class RAMDisplay extends JLabel implements ActionListener {

    private final Timer textUpdateTimer;
    private final Timer autoRepaintTimer;
    private final HardwareAbstractionLayer hal;
    private final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    private final DecimalFormat numberFormatter;

    public RAMDisplay() {
        super("RAM: --");

        SystemInfo si = new SystemInfo();
        hal = si.getHardware();

        numberFormatter = NumberUtil.buildNumberFormatter();

        setVisible(ApplicationConfig.DEV_MODE);
        setForeground(Colors.ACCENT_BLUE);
        setFont(getFont().deriveFont(Font.BOLD, 14f));
        setToolTipText("RAM");
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

        double currentRamValueMb = Math.floor(getRamValue());
        double maxRamValueMb = Math.floor(maxMemory());

        double onePerc = maxRamValueMb / 100;

        int availableRamValuePercentage = (int) (currentRamValueMb / onePerc);

        if (availableRamValuePercentage < 20) {
            setForeground(Color.GREEN);
        } else if (availableRamValuePercentage < 30) {
            setForeground(Colors.LIME);
        } else if (availableRamValuePercentage < 50) {
            setForeground(Color.ORANGE);
        } else if (availableRamValuePercentage < 70) {
            setForeground(Color.RED);
        }

        setText(String.format("RAM: %s%% | %sMb | %sMb", availableRamValuePercentage, numberFormatter.format(currentRamValueMb), numberFormatter.format(maxRamValueMb)));
    }

    private double getRamValue() {
        return (hal.getMemory().getTotal() - hal.getMemory().getAvailable()) / 1024 / 1024;
    }

    private double maxMemory() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        return hal.getMemory().getTotal() / 1024 / 1024;
    }

    public void stopAutoRepaint() {
        if (autoRepaintTimer.isRunning()) {
            autoRepaintTimer.stop();
        }
    }
}