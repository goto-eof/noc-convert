package org.andreidodu.nocconvert.gui.components;


import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.constants.Colors;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.Optional;

import static org.andreidodu.nocconvert.gui.constants.Colors.*;
import static org.andreidodu.nocconvert.gui.util.ShapeUtil.*;

public class ListItemRenderer extends JPanel implements ListCellRenderer<ConversionItemDTO> {
    private static final Logger log = LogManager.getLogger(ImageConverterServiceImpl.class);

    private final JLabel fileNameLabel;
    private final JLabel fileSize;
    private final JLabel statusLabel;
    private final JLabel targetFormat;
    private final JPanel statusPanel;
    private final JProgressBar progressBar;
    private final JPanel contentPanel;
    private final JPanel topPanel;
    private final JPanel wrapperPanel;

    private Color statusLabelBackground = LIME_DARK;

    public ListItemRenderer() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        fileNameLabel = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(false);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            }
        };
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD, 12f));

        statusLabel = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(false);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                GeneralPath path = getRoundedEastPath(width, height, 7);

                g2.setColor(statusLabelBackground);
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
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
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
        statusLabel.setPreferredSize(new Dimension(100, 10));
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(targetFormat, BorderLayout.WEST);

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
        topPanel.setBorder(new EmptyBorder(5, 0, 5, 0));


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


        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, LIST_BG));
        wrapperPanel.add(contentPanel, BorderLayout.CENTER);

        add(wrapperPanel, BorderLayout.CENTER);

        setOpaque(false);
        wrapperPanel.setOpaque(false);
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
        progressBar.setValue((int) value.getProgressPercentage());
        fileSize.setText(value.getFileSize());

        ConversionStatus status = value.getStatus();

        statusLabel.setText(status.name());
        Optional.ofNullable(value.getErrorMessage()).ifPresent(errorMessage -> {
            statusLabel.setToolTipText(errorMessage);
            log.info(errorMessage);
        });
        targetFormat.setText(value.getTargetFormat());


        statusLabelBackground = ConversionStatus.FAILED.equals(value.getStatus()) ? Colors.RED : LIME_DARK;

        Color border = list.getBackground(); // pari % 2 == 0 ? GRAY_DARK :
        border = isSelected ? LIME_SUPER_SUPER_DARK : GRAY_DARK;

        setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, border));

        Color statusColor = ConversionStatus.FAILED.equals(value.getStatus()) ? Colors.RED : ACCENT_DARK_GRAY;

        progressBar.setForeground(statusColor);

        fileNameLabel.setForeground(Color.WHITE);


//        setBackground(ACCENT_BLUE);
//        topPanel.setBackground(ACCENT_BLUE);
//        contentPanel.setBackground(ACCENT_BLUE);
//        fileNameLabel.setBackground(ACCENT_BLUE);
//        statusLabel.setBackground(ACCENT_BLUE);
//        progressBar.setBackground(ACCENT_BLUE);
        wrapperPanel.setBackground(GRAY_DARK);

        setOpaque(false);
        wrapperPanel.setOpaque(true);
        topPanel.setOpaque(false);
        contentPanel.setOpaque(false);
        fileNameLabel.setOpaque(false);
        statusLabel.setOpaque(false);
        progressBar.setOpaque(false);
//        pari++;
        return this;
    }
}
