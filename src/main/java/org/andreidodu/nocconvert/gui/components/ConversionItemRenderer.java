package org.andreidodu.nocconvert.gui.components;


import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;

import javax.swing.*;
import java.awt.*;

public class ConversionItemRenderer extends JPanel implements ListCellRenderer<ConversionItemDTO> {

    private final JLabel fileNameLabel;
    private final JLabel statusLabel;
    private final JLabel targetFormat;
    private final JPanel statusPanel;
    private final JProgressBar progressBar;
    private final JPanel contentPanel;
    private final JPanel topPanel;
    private static final Color DARK_ACCENT_BLUE = new Color(0, 120, 212, 189);
    private static final Color DARK_ACCENT_YELLOW =  new Color(212, 177, 0, 87);
    private static final Color DARK_ACCENT_GREEN =  new Color(102, 212, 0, 87);
    private static final Color DARK_ACCENT_GRAY =  new Color(30, 30, 30);
    private static final Color SELECTED_DARK_ACCENT_BLUE = new Color(0, 120, 212, 124);
    private static final Color ACCENT_BLUE = new Color(0, 120, 212);
    private static final Color ACCENT_GREEN = new Color(102, 212, 0);
    private static final Color ACCENT_GRAY = new Color(205, 203, 203);
    private static final Color ACCENT_YELLOW = new Color(212, 177, 0);

    public ConversionItemRenderer() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        fileNameLabel = new JLabel();
        fileNameLabel.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD, 12f));
        fileNameLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        statusLabel = new JLabel();
        targetFormat = new JLabel();
        targetFormat.setFont(targetFormat.getFont().deriveFont(Font.BOLD, 12f));
        targetFormat.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 10f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));



        statusPanel = new JPanel(new BorderLayout(3, 0));
        statusPanel.add(statusLabel, BorderLayout.EAST);
        statusPanel.add(new JLabel("-"), BorderLayout.CENTER);
        statusPanel.add(targetFormat, BorderLayout.WEST);

        progressBar = new JProgressBar(0, 100);
        progressBar.setFont(fileNameLabel.getFont().deriveFont(Font.BOLD, 8f));
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(100, 12));

        topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(true);
        topPanel.add(fileNameLabel, BorderLayout.WEST);
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


        add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ConversionItemDTO> list,
                                                  ConversionItemDTO value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {

        fileNameLabel.setText(value.getFileName());
        progressBar.setValue(value.getProgressPercentage());

        ConversionStatus status = value.getStatus();

        statusLabel.setText(status.name());
        targetFormat.setText(value.getTargetFormat());


        if (ConversionStatus.QUEUED.equals(status.status()) ||
                ConversionStatus.COMPLETED.equals(status.status()) ||
                ConversionStatus.FAILED.equals(status.status())) {
            progressBar.setVisible(false);
        } else {
            progressBar.setVisible(true);
            progressBar.setString(value.getProgressPercentage() + "%");
        }

        Color fg = new Color(255, 255, 255, 150);

        Color background = isSelected ? SELECTED_DARK_ACCENT_BLUE : list.getBackground();
        Color foreground = isSelected ? list.getSelectionForeground() : fg;

        setBackground(background);
        contentPanel.setBackground(background);
        Color bg = new Color(47, 50, 56);
        topPanel.setBackground(bg);
        contentPanel.setBackground(bg);
        progressBar.setForeground(DARK_ACCENT_BLUE);

        statusPanel.setBackground(bg);
        fileNameLabel.setBackground(bg);
        fileNameLabel.setForeground(foreground);
        statusLabel.setBackground(bg);
        statusLabel.setForeground(isSelected ? list.getSelectionForeground() : fg);



        // targetFormat.setForeground(isSelected ? list.getSelectionForeground() : fg);

        statusLabel.setBackground(DARK_ACCENT_GREEN);
        statusLabel.setForeground(ACCENT_GREEN);


        targetFormat.setForeground(ACCENT_GRAY);
        targetFormat.setBackground(DARK_ACCENT_GRAY);
        targetFormat.setOpaque(true);

        setOpaque(true);
        topPanel.setOpaque(true);
        contentPanel.setOpaque(true);
        fileNameLabel.setOpaque(true);
        statusLabel.setOpaque(true);
        progressBar.setOpaque(true);

        return this;
    }
}
