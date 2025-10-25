package org.andreidodu.nocconvert.gui.components;

import lombok.Getter;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Objects;

import static org.andreidodu.nocconvert.gui.constants.Colors.*;
import static org.andreidodu.nocconvert.gui.util.ShapeUtil.getRoundedEastPath;
import static org.andreidodu.nocconvert.gui.util.ShapeUtil.getRoundedWestPath;

public class SplitButtonComponent extends JPanel {

    @Getter
    private JButton mainActionButton;
    @Getter
    private JButton dropdownToggleButton;
    @Getter
    private FormatExtensionDTO selectedItem;

    private final GUIOrchestrator orchestrator;

    public SplitButtonComponent(GUIOrchestrator orchestrator) {
        Objects.requireNonNull(orchestrator);

        this.orchestrator = orchestrator;
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
    }

    public void init(String mainActionLabel, List<FormatExtensionDTO> menuList) {
        Objects.requireNonNull(mainActionLabel);
        Objects.requireNonNull(menuList);
        if (menuList.isEmpty()) {
            throw new IllegalArgumentException("The menu list is empty");
        }

        mainActionButton = buildMainActionButton(mainActionLabel);
        dropdownToggleButton = buildDropdownToggleButton(menuList);
        add(mainActionButton, BorderLayout.CENTER);
        add(dropdownToggleButton, BorderLayout.EAST);

    }

    private JPopupMenu getPopupMenu(List<FormatExtensionDTO> imageFormatList) {
        JPopupMenu popupMenu = new JPopupMenu();
        for (FormatExtensionDTO fileFormat : imageFormatList) {
            JMenuItem item = new JMenuItem(fileFormat.toString());
            item.setBackground(new Color(37, 37, 38));
            item.setForeground(TEXT_LIGHT);

            item.addActionListener(e -> {
                setMainActionLabel("CONVERT to " + fileFormat.getDescription());
                this.selectedItem = fileFormat;
                orchestrator.pack();
            });

            popupMenu.add(item);
        }
        FormatExtensionDTO selectedDefault = imageFormatList.getFirst();
        this.selectedItem = selectedDefault;
        setMainActionLabel("CONVERT to " + selectedDefault.getDescription());
        return popupMenu;
    }


    private JButton buildDropdownToggleButton(List<FormatExtensionDTO> menuList) {
        JButton button = buildCustomDropdownToggleButton();
        JPopupMenu menu = getPopupMenu(menuList);
        button.setOpaque(false);
        button.putClientProperty("JPopupMenu.showSeparator", true);
        button.addActionListener(e -> {
            menu.show(button, 0, button.getHeight());
        });
        return button;
    }

    private JButton buildCustomDropdownToggleButton() {
        return new JButton("▼") {
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
    }

    private JButton buildMainActionButton(String mainLabel) {
        return new JButton(mainLabel) {
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
    }


    public void setMainActionLabel(String label) {
        Objects.requireNonNull(label);

        mainActionButton.setText(label);
    }

}

