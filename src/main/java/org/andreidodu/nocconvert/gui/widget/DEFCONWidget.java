package org.andreidodu.nocconvert.gui.widget;

import org.andreidodu.nocconvert.constants.ApplicationConfig;
import org.andreidodu.nocconvert.enums.DEFCON;
import org.andreidodu.nocconvert.gui.constants.Colors;
import org.andreidodu.nocconvert.gui.worker.hybrid.SearchAndConvertWorker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DEFCONWidget extends JLabel implements ActionListener {

    private final Timer textUpdateTimer;
    private final Timer autoRepaintTimer;
    private SearchAndConvertWorker searchAndConvertWorker;

    public DEFCONWidget() {
        super("DEFCON: --");
        setVisible(ApplicationConfig.DEV_MODE);
        setForeground(Color.YELLOW);
        setFont(getFont().deriveFont(Font.BOLD, 14f));
        setToolTipText("DEFCON LEVEL");
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
        if (!ApplicationConfig.DEV_MODE || searchAndConvertWorker == null) {
            return;
        }

        DEFCON defconLevel = searchAndConvertWorker.getDefconLevel();

        if (DEFCON.LEVEL_3.equals(defconLevel)) {
            setForeground(Color.GREEN);
        } else if (DEFCON.LEVEL_2.equals(defconLevel)) {
            setForeground(Color.ORANGE);
        } else if (DEFCON.LEVEL_1.equals(defconLevel)) {
            setForeground(Color.RED);
        }

        if (DEFCON.LEVEL_3.equals(defconLevel)) {
            int defcon3SecondsToWait = searchAndConvertWorker.getDefcon3SecondsToWait();
            setText(String.format("DEFCON: %s | STW: %s", defconLevel.getLevel(), defcon3SecondsToWait));
            return;
        }

        setText(String.format("DEFCON: %s", defconLevel.getLevel()));
    }

    public void stopAutoRepaint() {
        if (autoRepaintTimer.isRunning()) {
            autoRepaintTimer.stop();
        }
    }

    public void setSource(SearchAndConvertWorker searchAndConvertWorker) {
        this.searchAndConvertWorker = searchAndConvertWorker;
    }
}