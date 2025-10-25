package org.andreidodu.nocconvert.gui.components;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.FormatExtensionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.GeneralPath;

import static org.andreidodu.nocconvert.gui.components.Colors.*;
import static org.andreidodu.nocconvert.gui.components.ShapeUtil.getRoundedEastPath;
import static org.andreidodu.nocconvert.gui.components.ShapeUtil.getRoundedWestPath;

public class ModernSplitButton extends JPanel {

    @Getter
    private final JButton mainActionButton;
    @Getter
    private final JButton dropdownToggleButton;
    @Getter
    private final JPopupMenu formatMenu;
    @Getter
    private FormatExtensionDTO format;


    private static final Color BORDER_DARK = new Color(60, 60, 60);
    private static final Color TEXT_LIGHT = new Color(255, 255, 255);

    public ModernSplitButton(JFrame parent, String mainLabel, FormatExtensionDTO[] formats) {
        setLayout(new BorderLayout(0, 0));
//        setBorder(BorderFactory.createLineBorder(LIME_DARK, 1));
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
                // Crea una copia dell'oggetto Graphics e lo converte in Graphics2D
                Graphics2D g2 = (Graphics2D) g.create();

                // PUNTO CRITICO 2: Abilita l'antialiasing per rendere gli angoli lisci e non seghettati
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // 1. Disegna lo sfondo arrotondato
                GeneralPath path = getRoundedWestPath(width, height);

                // --- 2. Riempimento e Disegno ---

                Color color = isHover ? LIME_SUPER_DARK : LIME_DARK;

                g2.setColor(color); // Un bel colore viola
                g2.fill(path); // Riempie la forma definita


                // Imposta il colore per il bordo
//                g2.setColor(Color.WHITE);
//                g2.setStroke(new BasicStroke(1));
//                g2.draw(path); // Disegna il bordo

                // Rilascia le risorse Graphics2D
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
                // Crea una copia dell'oggetto Graphics e lo converte in Graphics2D
                Graphics2D g2 = (Graphics2D) g.create();

                // PUNTO CRITICO 2: Abilita l'antialiasing per rendere gli angoli lisci e non seghettati
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // 1. Disegna lo sfondo arrotondato
                GeneralPath path = getRoundedEastPath(width, height);
                Color color = isHover ? LIME_SUPER_DARK : LIME_DARK;

                g2.setColor(color); // Un bel colore viola
                g2.fill(path); // Riempie la forma definita

                // Imposta il colore per il bordo
//                g2.setColor(Color.WHITE);
//                g2.setStroke(new BasicStroke(1));
//                g2.draw(path); // Disegna il bordo

                // Rilascia le risorse Graphics2D
                g2.dispose();
                super.paintComponent(g);
            }
        };
        dropdownToggleButton.setBackground(BG_FOOTER);
        dropdownToggleButton.setOpaque(false);


//        dropdownToggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
//            @Override
//            public void mouseEntered(java.awt.event.MouseEvent evt) {
//                if (dropdownToggleButton.isEnabled()) {
//                    dropdownToggleButton.setBackground(LIME_SUPER_DARK);
//                }
//            }
//
//            @Override
//            public void mouseExited(java.awt.event.MouseEvent evt) {
//                dropdownToggleButton.setBackground(LIME_DARK);
//            }
//        });

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

//    @Override
//    public void setEnabled(boolean enabled) {
//        super.setEnabled(enabled);
//        mainActionButton.setEnabled(enabled);
//        dropdownToggleButton.setEnabled(enabled);
//
//        if (!enabled) {
//            mainActionButton.setBackground(new Color(60, 60, 60));
//            dropdownToggleButton.setBackground(new Color(40, 40, 40));
//            setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
//        } else {
//            mainActionButton.setBackground(LIME_DARK);
//            dropdownToggleButton.setBackground(LIME_SUPER_DARK);
//            setBorder(BorderFactory.createLineBorder(LIME_DARK, 1));
//        }
//    }
}

