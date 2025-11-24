package org.andreidodu.nocconvert.gui.widget;

import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.enums.DEFCON;
import org.andreidodu.nocconvert.gui.constants.Colors;
import org.andreidodu.nocconvert.util.NumberUtil;
import org.andreidodu.nocconvert.util.RamUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

public class RAMWidget extends JLabel implements ActionListener {

    private final Timer textUpdateTimer;
    private final Timer autoRepaintTimer;
    private final DecimalFormat numberFormatter;
    private final RamUtil ramUtil;

    public RAMWidget() {
        super("RAM: --");
        ramUtil = new RamUtil();

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

        double currentRamValueMb = Math.floor(ramUtil.getConsumedRam());
        double maxRamValueMb = Math.floor(ramUtil.getMaxRam());

        double onePerc = maxRamValueMb / 100;

        int availableRamValuePercentage = (int) (currentRamValueMb / onePerc);

        if (availableRamValuePercentage >= DEFCON.LEVEL_1.getCriticalPoint()) {
            setForeground(Color.RED);
        } else if (availableRamValuePercentage >= DEFCON.LEVEL_2.getCriticalPoint()) {
            setForeground(Color.ORANGE);
        } else if (availableRamValuePercentage >= DEFCON.LEVEL_3.getCriticalPoint()) {
            setForeground(Color.GREEN);
        } else {
            setForeground(Colors.LIME);
        }

//        if (availableRamValuePercentage < 20) {
//            setForeground(Color.GREEN);
//        } else if (availableRamValuePercentage < 30) {
//            setForeground(Colors.LIME);
//        } else if (availableRamValuePercentage < 50) {
//            setForeground(Color.ORANGE);
//        } else if (availableRamValuePercentage < 70) {
//            setForeground(Color.RED);
//        }

        setText(String.format("RAM: %s%% | %sMb | %sMb", availableRamValuePercentage, numberFormatter.format(currentRamValueMb), numberFormatter.format(maxRamValueMb)));
    }


    public void stopAutoRepaint() {
        if (autoRepaintTimer.isRunning()) {
            autoRepaintTimer.stop();
        }
    }
}