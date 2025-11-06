package org.andreidodu.nocconvert.gui.components.performant;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static org.andreidodu.nocconvert.gui.constants.Colors.*;

class PerformantCellRenderer extends JPanel implements ListCellRenderer<ConversionItemDTO> {
    final Color ROW_EVEN_COLOR = LIST_BG3;
    final Color ROW_ODD_COLOR = BLUE_SUPER_SUPER_DARK;

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
    private final StringBuffer stringBuffer = new StringBuffer();


    @Getter
    private final int rowHeight = 70;

    public PerformantCellRenderer() {
        setLayout(new BorderLayout());

        fileNameLabel = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(true);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                setFont(this.getFont().deriveFont(Font.BOLD, 12f));
                setBackground(LIST_BG);
            }
        };

        statusLabel = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(false);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                setBackground(statusLabelBackground);
            }
        };

        targetFormat = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(true);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                setBackground(YELLOW_SUPER_DARK);
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
                setOpaque(true);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                setBackground(GRAY_SUPER_DARK);
            }
        };

        JPanel fileInfoPanel = new JPanel(new BorderLayout(0, 0));
        fileInfoPanel.add(fileNameLabel, BorderLayout.WEST);
        fileInfoPanel.add(fileSize, BorderLayout.CENTER);

        topPanel = new JPanel(new BorderLayout());
        topPanel.add(fileInfoPanel, BorderLayout.WEST);
        topPanel.add(statusPanel, BorderLayout.EAST);
        topPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
        topPanel.setOpaque(false);

        contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);


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
        wrapperPanel.add(contentPanel, BorderLayout.CENTER);
        wrapperPanel.setOpaque(false);

        add(wrapperPanel, BorderLayout.CENTER);

        errorMessage = new JTextArea();
        errorMessage.setWrapStyleWord(true);
        errorMessage.setLineWrap(true);
        errorMessage.setEnabled(false);
        errorMessage.setOpaque(false);
        errorMessage.setBackground(LIST_BG);
        errorMessage.setForeground(RED);
        errorMessage.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        wrapperPanel.add(errorMessage, BorderLayout.SOUTH);

        progressBar.setForeground(statusLabelBackground);

        fileNameLabel.setForeground(Color.WHITE);

        setBackground(GRAY_DARK);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ConversionItemDTO> list,
                                                  ConversionItemDTO currentItemDTO,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        if (isSelected) {
            setForeground(list.getSelectionForeground());
        } else {
            if (index % 2 == 0) {
                setBackground(ROW_EVEN_COLOR);
                fileNameLabel.setBackground(ROW_EVEN_COLOR);
            } else {
                setBackground(ROW_ODD_COLOR);
                fileNameLabel.setBackground(ROW_ODD_COLOR);
            }
            setForeground(list.getForeground());
        }


        stringBuffer.setLength(0);
        stringBuffer.append(index);
        stringBuffer.append(") ");
        stringBuffer.append(currentItemDTO.getFileName());
        fileNameLabel.setText(stringBuffer.toString());

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

        targetFormat.setText(currentItemDTO.getTargetFormat().toUpperCase());

        if (ConversionStatus.PROCESSING.equals(currentItemDTO.getStatus())) {
            statusLabelBackground = ACCENT_BLUE_DARK;
        } else if (ConversionStatus.COMPLETED.equals(currentItemDTO.getStatus())) {
            statusLabelBackground = LIME_DARK;
        } else if (ConversionStatus.QUEUED.equals(currentItemDTO.getStatus())) {
            statusLabelBackground = YELLOW_DARK;
        } else if (ConversionStatus.FAILED.equals(currentItemDTO.getStatus())) {
            statusLabelBackground = RED_DARK;
        }
        statusPanel.setBackground(statusLabelBackground);


        return this;
    }
}
