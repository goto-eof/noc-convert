package org.andreidodu.nocconvert.gui.components;


import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.GeneralPath;

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
    private final JTextArea errorMessage;
    private Color statusLabelBackground = LIME_SUPER_DARK;

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

                g2.setColor(YELLOW_SUPER_DARK);
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

        errorMessage = new JTextArea();
        errorMessage.setWrapStyleWord(true);
        errorMessage.setLineWrap(true);
        errorMessage.setEnabled(false);
        errorMessage.setOpaque(true);
        errorMessage.setBackground(LIST_BG);
        errorMessage.setForeground(RED);
        errorMessage.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        wrapperPanel.add(errorMessage, BorderLayout.SOUTH);

        setOpaque(false);
        wrapperPanel.setOpaque(false);
        topPanel.setOpaque(false);
        progressBar.setOpaque(false);
        contentPanel.setOpaque(false);
        fileInfoPanel.setOpaque(false);


    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ConversionItemDTO> list,
                                                  ConversionItemDTO currentItemDTO,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        fileNameLabel.setText((currentItemDTO.getIndex() + 1) + " | " + currentItemDTO.getFileName());
        progressBar.setValue(currentItemDTO.getProgressPercentage().intValue());
        fileSize.setText(currentItemDTO.getFileSize());

        ConversionStatus status = currentItemDTO.getStatus();

        statusLabel.setText(status.name());

        if (currentItemDTO.getErrorMessage() != null) {
            errorMessage.setText(currentItemDTO.getErrorMessage());
            errorMessage.setVisible(true);
        } else {
            errorMessage.setText("");
            errorMessage.setVisible(false);
        }

        targetFormat.setText(currentItemDTO.getTargetFormat());


        if (ConversionStatus.PROCESSING.equals(currentItemDTO.getStatus())) {
            statusLabelBackground = ACCENT_BLUE_DARK;
        }

        if (ConversionStatus.COMPLETED.equals(currentItemDTO.getStatus())) {
            statusLabelBackground = LIME_DARK;
        }

        if (ConversionStatus.QUEUED.equals(currentItemDTO.getStatus())) {
            statusLabelBackground = YELLOW_DARK;
        }

        statusLabelBackground = ConversionStatus.FAILED.equals(currentItemDTO.getStatus()) ? RED_DARK : statusLabelBackground;


        Color border = list.getBackground();
        border = isSelected ? LIME_SUPER_SUPER_DARK : GRAY_DARK;

        setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, border));

        progressBar.setForeground(statusLabelBackground);

        fileNameLabel.setForeground(Color.WHITE);

        wrapperPanel.setBackground(GRAY_DARK);

        setOpaque(false);
        errorMessage.setOpaque(false);
        wrapperPanel.setOpaque(true);
        topPanel.setOpaque(false);
        contentPanel.setOpaque(false);
        fileNameLabel.setOpaque(false);
        statusLabel.setOpaque(false);
        progressBar.setOpaque(false);
        return this;
    }
}
