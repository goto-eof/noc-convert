package org.andreidodu.nocconvert.gui.components;

import lombok.Getter;
import org.andreidodu.nocconvert.gui.constants.Colors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TextfieldDoubleButtonComponent extends TextfieldButtonComponent implements Colors {
    private static final float ARC_SIZE_BTN = 10;

    @Getter
    private final JButton secondaryButton;


    public TextfieldDoubleButtonComponent() {
        super();
        secondaryButton = createBrowseButton("Open");
    }

    public TextfieldDoubleButtonComponent(String initialText, String labelText, String buttonText) {
        super(initialText, labelText);
        secondaryButton = createBrowseButton(buttonText);
        super.getOnlyButtonsPanel().add(secondaryButton, BorderLayout.WEST);


        secondaryButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 3, new Color(255, 255, 255, 50)),
                new EmptyBorder(8, 0, 8, 0)
        ));

        secondaryButton.setPreferredSize(new Dimension(84, 38));

        getOnlyButtonsPanel().repaint();
    }


    private JButton createBrowseButton(String buttonText) {

        return new JButton(buttonText) {
            private boolean isHover = false;

            {
                SwingUtilities.invokeLater(() -> {
                    setContentAreaFilled(false);
                    setBorderPainted(true);
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

                Color color = isHover ? LIME_SUPER_DARK : LIME_DARK;
                color = isEnabled() ? color : DARK_ACCENT_GREEN;
                g2.setColor(color);
                g2.fill(westRounded);

                g2.dispose();
                super.paintComponent(g);
            }


        };
    }

}
