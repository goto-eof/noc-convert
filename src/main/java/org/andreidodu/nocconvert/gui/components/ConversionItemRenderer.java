package org.andreidodu.nocconvert.gui.components;


import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

import static org.andreidodu.nocconvert.gui.constants.Colors.*;
import static org.andreidodu.nocconvert.gui.util.ShapeUtil.*;

public class ConversionItemRenderer extends JPanel implements ListCellRenderer<ConversionItemDTO> {

    private final JLabel fileNameLabel;
    private final JLabel fileSize;
    private final JLabel statusLabel;
    private final JLabel targetFormat;
    private final JPanel statusPanel;
    private final JProgressBar progressBar;
    private final JPanel contentPanel;
    private final JPanel topPanel;

    int pari = 0;

    public ConversionItemRenderer() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        fileNameLabel = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(false);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        };
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD, 12f));
        fileNameLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        statusLabel = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(false);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                GeneralPath path = getRoundedEastPath(width, height, 7);

                g2.setColor(LIME_DARK);
                g2.fill(path);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        targetFormat = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(false);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                GeneralPath path = getRoundedWestPath(width, height, 7);

                g2.setColor(YELLOW_DARK);
                g2.fill(path);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusPanel = new JPanel(new BorderLayout(0, 0));
        statusPanel.add(statusLabel, BorderLayout.EAST);
        statusPanel.add(targetFormat, BorderLayout.CENTER);

        progressBar = new JProgressBar(0, 100);
        progressBar.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD, 8f));
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(100, 12));

        fileSize = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(false);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                GeneralPath path = getRoundedPath(width, height, 10);

                g2.setColor(GRAY_SUPER_DARK);
                g2.fill(path);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        JPanel fileInfoPanel = new JPanel(new BorderLayout(0, 0));
        fileInfoPanel.add(fileNameLabel, BorderLayout.WEST);
        fileInfoPanel.add(fileSize, BorderLayout.CENTER);

        topPanel = new JPanel(new BorderLayout());
        topPanel.add(fileInfoPanel, BorderLayout.WEST);
        topPanel.add(statusPanel, BorderLayout.EAST);


        contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(true);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 2, 0);
        contentPanel.add(topPanel, gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(progressBar, gbc);


        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        add(contentPanel, BorderLayout.CENTER);
        setOpaque(false);
        topPanel.setOpaque(false);
        progressBar.setOpaque(false);
        contentPanel.setOpaque(false);
        fileInfoPanel.setOpaque(false);


    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ConversionItemDTO> list,
                                                  ConversionItemDTO value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        fileNameLabel.setText(value.getFileName());
        progressBar.setValue(value.getProgressPercentage());
        fileSize.setText(value.getFileSize() + "Kb");

        ConversionStatus status = value.getStatus();

        statusLabel.setText(status.name());
        targetFormat.setText(value.getTargetFormat());


        Color border = pari % 2 == 0 ? GRAY_DARK : list.getBackground();
        border = isSelected ? LIME_SUPER_SUPER_DARK : border;

        setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, border));

        setBackground(border);
        progressBar.setForeground(LIME_DARK);

        fileNameLabel.setForeground(Color.WHITE);


        setOpaque(true);
        topPanel.setOpaque(false);
        contentPanel.setOpaque(false);
        fileNameLabel.setOpaque(false);
        statusLabel.setOpaque(false);
        progressBar.setOpaque(false);
        pari++;
        return this;
    }
}
