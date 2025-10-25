package org.andreidodu.nocconvert.gui.components;


import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;

import static org.andreidodu.nocconvert.gui.components.Colors.*;
import static org.andreidodu.nocconvert.gui.components.ShapeUtil.*;

public class ConversionItemRenderer extends JPanel implements ListCellRenderer<ConversionItemDTO> {

    private final JLabel fileNameLabel;
    private final JLabel fileSize;
    private final JLabel statusLabel;
    private final JLabel targetFormat;
    private final JPanel statusPanel;
    private final JProgressBar progressBar;
    private final JPanel contentPanel;
    private final JPanel topPanel;
    private static final Color DARK_ACCENT_BLUE = new Color(0, 120, 212, 189);

    private static final Color DARK_ACCENT_GREEN = new Color(102, 212, 0, 87);
    private static final Color DARK_ACCENT_GRAY = new Color(30, 30, 30);
    private static final Color SELECTED_DARK_ACCENT_BLUE = new Color(0, 120, 212, 124);
    private static final Color ACCENT_BLUE = new Color(0, 120, 212);
    private static final Color ACCENT_GREEN = new Color(102, 212, 0);
    private static final Color ACCENT_GRAY = new Color(205, 203, 203);
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

//            @Override
//            protected void paintComponent(Graphics g) {
//                // Crea una copia dell'oggetto Graphics e lo converte in Graphics2D
//                Graphics2D g2 = (Graphics2D) g.create();
//
//                // PUNTO CRITICO 2: Abilita l'antialiasing per rendere gli angoli lisci e non seghettati
//                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//                int width = getWidth();
//                int height = getHeight();
//
//                // 1. Disegna lo sfondo arrotondato
//                GeneralPath path = getRoundedWestPath(width, height, 7);
//
//                g2.setColor( new Color(41, 43, 46)); // Un bel colore viola
//                g2.fill(path); // Riempie la forma definita
//
//                // Imposta il colore per il bordo
////                g2.setColor(Color.WHITE);
////                g2.setStroke(new BasicStroke(1));
////                g2.draw(path); // Disegna il bordo
//
//                // Rilascia le risorse Graphics2D
//                g2.dispose();
//                super.paintComponent(g);
//            }
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
                // Crea una copia dell'oggetto Graphics e lo converte in Graphics2D
                Graphics2D g2 = (Graphics2D) g.create();

                // PUNTO CRITICO 2: Abilita l'antialiasing per rendere gli angoli lisci e non seghettati
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // 1. Disegna lo sfondo arrotondato
                GeneralPath path = getRoundedEastPath(width, height, 7);

                g2.setColor(LIME_DARK); // Un bel colore viola
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
        targetFormat = new JLabel() {
            {
                setFont(getFont().deriveFont(Font.BOLD, 10f));
                setOpaque(false);
                setForeground(Color.WHITE);
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
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
                GeneralPath path = getRoundedWestPath(width, height, 7);

                g2.setColor(YELLOW_DARK); // Un bel colore viola
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
//        targetFormat.setFont(targetFormat.getFont().deriveFont(Font.BOLD, 12f));
//        targetFormat.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


//        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


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
                // Crea una copia dell'oggetto Graphics e lo converte in Graphics2D
                Graphics2D g2 = (Graphics2D) g.create();

                // PUNTO CRITICO 2: Abilita l'antialiasing per rendere gli angoli lisci e non seghettati
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int height = getHeight();

                // 1. Disegna lo sfondo arrotondato
                GeneralPath path = getRoundedPath(width, height, 10);

                g2.setColor(GRAY_SUPER_DARK); // Un bel colore viola
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


        if (ConversionStatus.QUEUED.equals(status.status()) ||
                ConversionStatus.COMPLETED.equals(status.status()) ||
                ConversionStatus.FAILED.equals(status.status())) {
            progressBar.setVisible(false);
        } else {
            progressBar.setVisible(true);
            progressBar.setString(value.getProgressPercentage() + "%");
        }

        Color fg = new Color(255, 255, 255, 150);

        Color background = isSelected ? GRAY_SUPER_DARK : list.getBackground();
        Color foreground = isSelected ? list.getSelectionForeground() : fg;

        Color border = pari % 2 == 0 ? GRAY_DARK : list.getBackground();

        setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, border));

        setBackground(border);
//        contentPanel.setBackground(border);
//        Color bg = new Color(41, 43, 46);
//        topPanel.setBackground(bg);
//        contentPanel.setBackground(bg);
        progressBar.setForeground(LIME_DARK);

//        statusPanel.setBackground(bg);
//        statusLabel.setBackground(bg);
//        fileNameLabel.setBackground(bg);
        fileNameLabel.setForeground(Color.WHITE);
        //statusLabel.setForeground(isSelected ? list.getSelectionForeground() : fg);


        // targetFormat.setForeground(isSelected ? list.getSelectionForeground() : fg);
//
//        statusLabel.setBackground(DARK_ACCENT_GREEN);
        //statusLabel.setForeground(ACCENT_GREEN);


//        targetFormat.setForeground(ACCENT_GRAY);
//        targetFormat.setBackground(DARK_ACCENT_GRAY);
//        targetFormat.setOpaque(true);

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
