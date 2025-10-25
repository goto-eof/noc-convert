package org.andreidodu.nocconvert.gui.components;

import lombok.Getter;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.GeneralPath;

import static org.andreidodu.nocconvert.gui.constants.Colors.*;
import static org.andreidodu.nocconvert.gui.util.ShapeUtil.getRoundedEastPath;
import static org.andreidodu.nocconvert.gui.util.ShapeUtil.getRoundedWestPath;

public class SplitButtonComponent extends JPanel {

    @Getter
    private final JButton mainActionButton;
    @Getter
    private final JButton dropdownToggleButton;
    @Getter
    private final JPopupMenu formatMenu;
    @Getter
    private FormatExtensionDTO format;



    public SplitButtonComponent(JFrame parent, String mainLabel, FormatExtensionDTO[] formats) {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        mainActionButton = new JButton(mainLabel) {
            private boolean isHover;

            {
                setContentAreaFilled(false);
                setBorderPainted(false);


                setForeground(TEXT_LIGHT);
                setFont(getFont().deriveFont(Font.BOLD, 14f));
                setBorder(new EmptyBorder(15, 15, 15, 15));
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setOpaque(false);
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        isHover = true;
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        isHover = false;
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                GeneralPath path = getRoundedWestPath(width, height);

                Color color = isHover ? LIME_SUPER_DARK : LIME_DARK;

                g2.setColor(color);
                g2.fill(path);

                g2.dispose();
                super.paintComponent(g);

            }
        };


        dropdownToggleButton = new JButton("▼") {
            private boolean isHover;

            {
                setForeground(TEXT_LIGHT);
                setContentAreaFilled(false);
                setBorderPainted(true);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 50)),
                        new EmptyBorder(8, 10, 8, 10)
                ));
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setOpaque(false);
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        isHover = true;
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        isHover = false;
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                GeneralPath path = getRoundedEastPath(width, height);
                Color color = isHover ? LIME_SUPER_DARK : LIME_DARK;

                g2.setColor(color);
                g2.fill(path);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        dropdownToggleButton.setBackground(BG_FOOTER);
        dropdownToggleButton.setOpaque(false);

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

}

