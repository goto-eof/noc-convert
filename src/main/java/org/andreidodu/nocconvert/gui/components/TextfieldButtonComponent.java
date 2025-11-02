package org.andreidodu.nocconvert.gui.components;

import lombok.Getter;
import org.andreidodu.nocconvert.gui.constants.Colors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import static org.andreidodu.nocconvert.gui.util.ShapeUtil.getRoundedWestPath;

public class TextfieldButtonComponent extends JPanel implements Colors {

    public static final float ARC_SIZE = 20;
    private static final float ARC_SIZE_BTN = 10;
    @Getter
    private final JTextField textField;
    @Getter
    private final JButton button;

    @Getter
    private JPanel onlyButtonsPanel;

    public TextfieldButtonComponent() {
        textField = new JTextField("");
        button = createBrowseButton();
    }

    public TextfieldButtonComponent(String initialText, String labelText) {
        setLayout(new BorderLayout());


        textField = new JTextField(initialText);
        textField.setOpaque(false);
        textField.setBackground(LIST_BG);
        textField.setForeground(TEXT_LIGHT);
        textField.setCaretColor(TEXT_LIGHT);

        textField.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));

        textField.setFocusable(false);
        button = createBrowseButton();
        button.setOpaque(false);

        JLabel label = new JLabel(labelText + ":");
        label.setBackground(LIST_BG2);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));

        label.setOpaque(false);
        label.setPreferredSize(new Dimension(100, 20));

        JPanel labelAndTextFieldPanel = new JPanel();
        labelAndTextFieldPanel.setBackground(LIST_BG);
        labelAndTextFieldPanel.setOpaque(false);

        labelAndTextFieldPanel.add(label, BorderLayout.NORTH);
        labelAndTextFieldPanel.add(textField, BorderLayout.CENTER);
        labelAndTextFieldPanel.setOpaque(false);


        JPanel buttonPanel = new JPanel(new BorderLayout(0, 0));
        buttonPanel.setBackground(LIST_BG);
        buttonPanel.setOpaque(false);

        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setForeground(BORDER_DARK);
        separator.setSize(new Dimension(1, Integer.MAX_VALUE));
        separator.setMaximumSize(new Dimension(1, Integer.MAX_VALUE));
        separator.setPreferredSize(new Dimension(1, Integer.MAX_VALUE));
        buttonPanel.add(separator, BorderLayout.CENTER);


        onlyButtonsPanel = new JPanel(new BorderLayout(5, 0));
        onlyButtonsPanel.add(button, BorderLayout.EAST);

        buttonPanel.add(onlyButtonsPanel, BorderLayout.EAST);

        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel old = new JPanel(new BorderLayout(0, 0));
        old.add(buttonPanel, BorderLayout.EAST);
        old.setBorder(new EmptyBorder(10, 10, 10, 10));
        old.setOpaque(false);

        JPanel onlyLabel = new JPanel(new BorderLayout(0, 0)) {

            {

                setForeground(Color.WHITE);
                setOpaque(false);
                setFont(new Font("Arial", Font.BOLD, 14));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                GeneralPath path = getRoundedWestPath(width - 1, height - 1);


                g2.setColor(LIST_BG_LIGHTER);
                g2.fill(path);

                g2.setColor(Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1));
                g2.draw(path);

                g2.dispose();
                super.paintComponent(g);

            }
        };
        onlyLabel.add(label, BorderLayout.CENTER);
        onlyLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        onlyLabel.setOpaque(false);

        add(old, BorderLayout.EAST);
        add(textField, BorderLayout.CENTER);
        add(onlyLabel, BorderLayout.WEST);

        setOpaque(false);

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
            public void setEnabled(boolean b) {
                super.setEnabled(b);
                if (!b) {
                    isHover = false;
                    repaint();
                }
            }


            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getWidth();
                int height = getHeight();

                Color color = isHover ? LIME_SUPER_DARK : LIME_DARK;
                color = isEnabled() ? color : GRAY_DARK;
                g2.setColor(color);

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
        button.setEnabled(enabled);

        Color disabledColor = enabled ? Color.WHITE : new Color(150, 150, 150);
        textField.setForeground(TEXT_LIGHT);
        button.setForeground(disabledColor);

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
