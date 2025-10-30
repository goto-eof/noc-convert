package org.andreidodu.nocconvert.gui.components;

import lombok.Getter;
import org.andreidodu.nocconvert.gui.constants.Colors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class ButtonComponent extends JPanel implements Colors {

    public static final float ARC_SIZE = 20;
    private static final float ARC_SIZE_BTN = 10;
    @Getter
    private final JButton button;

    public ButtonComponent(String initialText) {
        setLayout(new BorderLayout());
        button = createButton(initialText);
        button.setOpaque(false);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        add(button, BorderLayout.CENTER);
        setOpaque(false);
        setEnabled(true);
    }

    private JButton createButton(String label) {

        return new JButton(label) {
            private boolean isHover = false;

            {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setForeground(Color.WHITE);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setOpaque(false);
                setFont(getFont().deriveFont(Font.BOLD, 14f));
                setBorder(new EmptyBorder(10, 15, 10, 15));
                setPreferredSize(new Dimension(280, 40));
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
    }


//    @Override
//    public void setEnabled(boolean enabled) {
//        super.setEnabled(enabled);
//        button.setEnabled(enabled);
//
//        Color disabledColor = enabled ? Color.WHITE : new Color(150, 150, 150);
//        button.setForeground(disabledColor);
//
//        if (!enabled) {
//            setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
//        }
//    }
//
//
//    @Override
//    protected void paintComponent(Graphics g) {
//        super.paintComponent(g);
//
//        Graphics2D g2 = (Graphics2D) g.create();
//
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//        int width = getWidth();
//        int height = getHeight();
//
//        g2.setColor(LIST_BG);
//        g2.fill(new RoundRectangle2D.Float(
//                0, 0, width, height, ARC_SIZE, ARC_SIZE
//        ));
//
//        g2.dispose();
//    }
//
//    @Override
//    protected void paintBorder(Graphics g) {
//        Graphics2D g2 = (Graphics2D) g.create();
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//        g2.setColor(LIST_BG2);
//        g2.setStroke(new BasicStroke(1));
//        g2.draw(new RoundRectangle2D.Float(
//                0, 0, getWidth() - 1, getHeight() - 1, ARC_SIZE, ARC_SIZE
//        ));
//
//        g2.dispose();
//    }

}
