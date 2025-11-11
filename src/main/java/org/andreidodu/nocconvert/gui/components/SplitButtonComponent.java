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
    private final JButton mainActionButton;
    @Getter
    private JButton dropdownToggleButton;
    @Getter
    private FormatExtensionDTO selectedItem;

    @Getter
    private Action action;

    private final GUIOrchestrator orchestrator;

    public SplitButtonComponent(GUIOrchestrator orchestrator) {

        Objects.requireNonNull(orchestrator);
        mainActionButton = buildMainActionButton();
        dropdownToggleButton = buildCustomDropdownToggleButton();

        this.orchestrator = orchestrator;
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        add(mainActionButton, BorderLayout.CENTER);
        add(dropdownToggleButton, BorderLayout.EAST);
    }

    public void setImageList(List<FormatExtensionDTO> menuList) {
        Objects.requireNonNull(menuList);
        if (menuList.isEmpty()) {
            throw new IllegalArgumentException("The menu list is empty");
        }

        dropdownToggleButton = buildDropdownToggleMenu(menuList);
    }

    public void setPreferredFormat(FormatExtensionDTO preferredFormat) {
        this.selectedItem = preferredFormat;
    }

    public void updateAction(Action action) {
        this.action = action;
        String additionalInfo = selectedItem.getExtension().equalsIgnoreCase(selectedItem.getFormat()) ? "" : " (" + selectedItem.getFormat().toUpperCase() + ")";
        if (Action.START.equals(action)) {
            setMainActionLabel("CONVERT to " + selectedItem.getDescription() + additionalInfo);
        } else if (Action.STOP.equals(action)) {
            setMainActionLabel("STOP CONVERSION to " + selectedItem.getExtension().toUpperCase() + additionalInfo);
        }
        mainActionButton.repaint();
        dropdownToggleButton.repaint();
        orchestrator.pack();
    }

    private JPopupMenu getPopupMenu(List<FormatExtensionDTO> imageFormatList) {
        JPopupMenu popupMenu = new JPopupMenu();
        for (FormatExtensionDTO fileFormat : imageFormatList) {
            JMenuItem item = new JMenuItem(fileFormat.toString());
            item.setBackground(new Color(37, 37, 38));
            item.setForeground(TEXT_LIGHT);

            item.addActionListener(e -> {
                this.selectedItem = fileFormat;
                updateAction(action);
            });
            popupMenu.add(item);
        }
        return popupMenu;
    }


    private JButton buildDropdownToggleMenu(List<FormatExtensionDTO> menuList) {
        JPopupMenu menu = getPopupMenu(menuList);
        dropdownToggleButton.setOpaque(false);
        dropdownToggleButton.putClientProperty("JPopupMenu.showSeparator", true);
        dropdownToggleButton.addActionListener(e -> {
            menu.show(dropdownToggleButton, dropdownToggleButton.getWidth(), -300);
        });
        return dropdownToggleButton;
    }

    private JButton buildCustomDropdownToggleButton() {
        return new JButton("▼") {
            private boolean isHover;

            {
                SwingUtilities.invokeLater(() -> {
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
                });

                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        isHover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
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

                GeneralPath path = getRoundedEastPath(width, height);
                Color color = isHover ? LIME_SUPER_DARK : LIME_DARK;
                color = isEnabled() ? color : GRAY_DARK;

                g2.setColor(color);
                g2.fill(path);

                g2.dispose();
                super.paintComponent(g);
            }
        };
    }

    private JButton buildMainActionButton() {
        return new JButton() {
            private boolean isHover;

            {
                SwingUtilities.invokeLater(() -> {
                    setContentAreaFilled(false);
                    setBorderPainted(false);
                    setForeground(TEXT_LIGHT);
                    setFont(getFont().deriveFont(Font.BOLD, 14f));
                    setBorder(new EmptyBorder(10, 15, 10, 15));
                    setPreferredSize(new Dimension(280, 40));
                    setFocusPainted(false);
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    setOpaque(false);
                });

                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        isHover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent evt) {
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

                GeneralPath path = getRoundedWestPath(width, height);

                Color dark = LIME_SUPER_DARK;
                Color light = LIME_DARK;

                if (Action.STOP.equals(action)) {
                    dark = RED_DARK;
                    light = RED;
                }

                Color color = isHover ? dark : light;
                color = isEnabled() ? color : GRAY_DARK;


                g2.setColor(color);
                g2.fill(path);

                g2.dispose();
                super.paintComponent(g);

            }
        };
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.getDropdownToggleButton().setEnabled(enabled);
        this.getMainActionButton().setEnabled(enabled);
    }


    public void setMainActionLabel(String label) {
        Objects.requireNonNull(label);

        mainActionButton.setText(label);
    }


    public enum Action {
        START, STOP
    }

}

