package org.andreidodu.nocconvert.gui.components;

import javax.swing.*;
import java.awt.*;

public class ModernInputButtonPanel extends JPanel {

    private final JTextField textField;
    private final JButton browseButton;

    //private static final Color LIST_BG = new Color(61, 64, 69);
    //private static final Color LIST_BG = new Color(27, 28, 30,  87);
    private static final Color LIST_BG = new Color(24, 25, 27, 166);
    private static final Color BUTTON_BG = new Color(47, 50, 57);
    private static final Color BORDER_DARK = new Color(60, 60, 60);
    private static final Color ACCENT_BLUE = new Color(0, 120, 212);
    private static final Color TEXT_LIGHT = new Color(212, 212, 212);
    private static final int BORDER_RADIUS = 6;
    private final String initialText = "";
    private static final Color ACCENT_BLUE_DARK = new Color(0, 90, 158);


    public ModernInputButtonPanel() {
        textField = new JTextField(initialText);
        browseButton = new JButton("Sfoglia");
    }

    public ModernInputButtonPanel(String initialText) {
        setLayout(new BorderLayout(0, 0));

        setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));

        setOpaque(false);

        textField = new JTextField(initialText);
        textField.setBackground(LIST_BG);
        textField.setForeground(TEXT_LIGHT);
        textField.setCaretColor(TEXT_LIGHT);

        textField.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));

        textField.setFocusable(false);

        browseButton = new JButton("Browse");
        browseButton.setBackground(ACCENT_BLUE);
        browseButton.setForeground(TEXT_LIGHT);

        browseButton.setMargin(new Insets(0, 0, 0, 0));
        browseButton.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));


        add(textField, BorderLayout.CENTER);


        JPanel buttonPanel = new JPanel(new BorderLayout(0, 0));

        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setForeground(BORDER_DARK);
        separator.setSize(new Dimension(1, Integer.MAX_VALUE));
        separator.setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
        separator.setPreferredSize(new Dimension(1, Integer.MAX_VALUE));
        buttonPanel.add(separator, BorderLayout.CENTER);
        buttonPanel.add(browseButton, BorderLayout.EAST);

        add(buttonPanel, BorderLayout.EAST);

        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
            }
        });

        browseButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (browseButton.isEnabled()) {
                    browseButton.setBackground(ACCENT_BLUE_DARK);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                browseButton.setBackground(ACCENT_BLUE);
            }
        });

        setEnabled(true);
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public String getText() {
        return textField.getText();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        browseButton.setEnabled(enabled);

        Color disabledColor = enabled ? TEXT_LIGHT : new Color(150, 150, 150);
        textField.setForeground(disabledColor);
        browseButton.setForeground(disabledColor);

        if (!enabled) {
            setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
        }
    }

}
