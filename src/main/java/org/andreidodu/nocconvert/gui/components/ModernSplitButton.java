package org.andreidodu.nocconvert.gui.components;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.FormatExtensionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ModernSplitButton extends JPanel {

    @Getter
    private final JButton mainActionButton;
    @Getter
    private final JButton dropdownToggleButton;
    @Getter
    private final JPopupMenu formatMenu;
    @Getter
    private FormatExtensionDTO format;

    private static final Color ACCENT_BLUE = new Color(0, 120, 212);
    private static final Color ACCENT_BLUE_DARK = new Color(0, 90, 158);
    private static final Color DROPDOWN_TOGGLE_BG = new Color(0, 90, 158);
    private static final Color DROPDOWN_TOGGLE_HOVER = new Color(0, 70, 125);
    private static final Color BORDER_DARK = new Color(60, 60, 60);
    private static final Color TEXT_LIGHT = new Color(255, 255, 255);

    public ModernSplitButton(JFrame parent, String mainLabel, FormatExtensionDTO[] formats) {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1));

        mainActionButton = new JButton(mainLabel);
        mainActionButton.setBackground(ACCENT_BLUE);
        mainActionButton.setForeground(TEXT_LIGHT);
        mainActionButton.setFont(mainActionButton.getFont().deriveFont(Font.BOLD, 14f));
        mainActionButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        mainActionButton.setFocusPainted(false);
        mainActionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        mainActionButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (mainActionButton.isEnabled()) {
                    mainActionButton.setBackground(ACCENT_BLUE_DARK);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                mainActionButton.setBackground(ACCENT_BLUE);
            }
        });

        dropdownToggleButton = new JButton("▼");
        dropdownToggleButton.setBackground(DROPDOWN_TOGGLE_BG);
        dropdownToggleButton.setForeground(TEXT_LIGHT);

        dropdownToggleButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 50)),
                new EmptyBorder(8, 10, 8, 10)
        ));
        dropdownToggleButton.setFocusPainted(false);
        dropdownToggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        dropdownToggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (dropdownToggleButton.isEnabled()) {
                    dropdownToggleButton.setBackground(DROPDOWN_TOGGLE_HOVER);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                dropdownToggleButton.setBackground(DROPDOWN_TOGGLE_BG);
            }
        });

        formatMenu = new JPopupMenu();
        formatMenu.setBorder(BorderFactory.createLineBorder(BORDER_DARK));

        formatMenu.putClientProperty("JPopupMenu.showSeparator", true);

        for (FormatExtensionDTO format : formats) {
            JMenuItem item = new JMenuItem(format.toString());
            item.setBackground(new Color(37, 37, 38));
            item.setForeground(TEXT_LIGHT);

            item.addActionListener(e -> {
                setMainActionLabel("CONVERT to " + format.getDescription());
                this.format = format;
                parent.pack();
            });
            formatMenu.add(item);
        }

        dropdownToggleButton.addActionListener(e -> {
            formatMenu.show(dropdownToggleButton, 0, dropdownToggleButton.getHeight());
        });

        add(mainActionButton, BorderLayout.CENTER);
        add(dropdownToggleButton, BorderLayout.EAST);
    }

    public void setMainActionLabel(String label) {
        mainActionButton.setText(label);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mainActionButton.setEnabled(enabled);
        dropdownToggleButton.setEnabled(enabled);

        if (!enabled) {
            mainActionButton.setBackground(new Color(60, 60, 60));
            dropdownToggleButton.setBackground(new Color(40, 40, 40));
            setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
        } else {
            mainActionButton.setBackground(ACCENT_BLUE);
            dropdownToggleButton.setBackground(DROPDOWN_TOGGLE_BG);
            setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1));
        }
    }
}

