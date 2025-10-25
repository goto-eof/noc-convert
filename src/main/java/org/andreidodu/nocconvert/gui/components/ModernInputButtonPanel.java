package org.andreidodu.nocconvert.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class ModernInputButtonPanel extends JPanel implements Colors {

    public static final float ARC_SIZE = 20;
    private static final float ARC_SIZE_BTN = 10;
    private final JTextField textField;
    private final JButton browseButton;
    private final boolean browseButtonHover = false;
    private static final Color LIST_BG = new Color(57, 60, 64);
    private static final Color LIST_BG2 = new Color(68, 71, 76);
    private static final Color BUTTON_BG = new Color(47, 50, 57);
    private static final Color BORDER_DARK = new Color(60, 60, 60);

    private static final Color TEXT_LIGHT = new Color(212, 212, 212);
    private static final Color TEXT_LIGHT2 = new Color(227, 227, 227);
    private static final Color TEXT_DARK = new Color(27, 34, 0);
    private static final int BORDER_RADIUS = 6;
    private final String initialText = "";


    public ModernInputButtonPanel() {
        textField = new JTextField(initialText);
        browseButton = createBrowseButton();

    }

    public ModernInputButtonPanel(String initialText, String labelText) {
        setLayout(new BorderLayout());


        textField = new JTextField(initialText);
        textField.setOpaque(false);
        textField.setBackground(LIST_BG);
        textField.setForeground(TEXT_LIGHT);
        textField.setCaretColor(TEXT_LIGHT);

        textField.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));

        textField.setFocusable(false);
        browseButton = createBrowseButton();
        browseButton.setOpaque(false);

        JLabel label = new JLabel(labelText + ":");
        label.setBackground(LIST_BG2);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));

        label.setOpaque(false);
        label.setPreferredSize(new Dimension(100, 20));

        JPanel buttonPanel2 = new JPanel();
        buttonPanel2.setBackground(LIST_BG);
        buttonPanel2.setOpaque(false);

        buttonPanel2.add(label, BorderLayout.NORTH);
        buttonPanel2.add(textField, BorderLayout.CENTER);
        buttonPanel2.setOpaque(false);

        add(buttonPanel2, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new BorderLayout(0, 0));
        buttonPanel.setBackground(LIST_BG);
        buttonPanel.setOpaque(false);

        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setForeground(BORDER_DARK);
        separator.setSize(new Dimension(1, Integer.MAX_VALUE));
        separator.setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
        separator.setPreferredSize(new Dimension(1, Integer.MAX_VALUE));
        buttonPanel.add(separator, BorderLayout.CENTER);
        buttonPanel.add(browseButton, BorderLayout.EAST);
        browseButton.setVerticalAlignment(SwingConstants.CENTER);
        browseButton.setHorizontalAlignment(SwingConstants.CENTER);

        add(buttonPanel, BorderLayout.EAST);

        setOpaque(false);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                setBorder(BorderFactory.createLineBorder(LIME, 1, true));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1, true));

            }
        });

        setEnabled(true);


    }

    private JButton createBrowseButton() {
        JButton button = new JButton("Browse") {
            private boolean isHover = false;

            {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setForeground(Color.WHITE);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setOpaque(false);
                setFont(new Font("Arial", Font.BOLD, 14));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent evt) {
                        isHover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent evt) {
                        isHover = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getWidth();
                int height = getHeight();
                if (isHover) {
                    g2.setColor(LIME_SUPER_DARK);
                } else {
                    g2.setColor(LIME_DARK);
                }
                g2.fill(new RoundRectangle2D.Float(
                        0, 0, width, height, ARC_SIZE_BTN, ARC_SIZE_BTN
                ));

                g2.dispose();
                super.paintComponent(g);

            }


        };

        return button;
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

        Color disabledColor = enabled ? Color.WHITE : new Color(150, 150, 150);
        textField.setForeground(TEXT_LIGHT);
        browseButton.setForeground(disabledColor);

        if (!enabled) {
            setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        g2.setColor(LIST_BG);
        g2.fill(new RoundRectangle2D.Float(
                0, 0, width, height, ARC_SIZE, ARC_SIZE
        ));

        g2.dispose();
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(LIST_BG2);
        g2.setStroke(new BasicStroke(1));
        g2.draw(new RoundRectangle2D.Float(
                0, 0, getWidth() - 1, getHeight() - 1, ARC_SIZE, ARC_SIZE
        ));

        g2.dispose();
    }

}
