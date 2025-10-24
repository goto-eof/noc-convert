package org.andreidodu.nocconvert.gui.components;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.FormatExtensionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Questa classe crea un componente Split Button personalizzato per Swing,
 * unendo l'azione principale e il pulsante del menu a tendina (dropdown)
 * in un unico contenitore con un design Dark Mode coeso, ispirato ai mockup.
 */

public class ModernSplitButton extends JPanel {

    @Getter
    private final JButton mainActionButton;
    @Getter
    private final JButton dropdownToggleButton;
    @Getter
    private final JPopupMenu formatMenu;
    @Getter
    private FormatExtensionDTO format;
    // Colori e stili (Adattati dalla palette Dark Mode)
    private static final Color ACCENT_BLUE = new Color(0, 120, 212);    // Blu Accent (Azione principale)
    private static final Color ACCENT_BLUE_DARK = new Color(0, 90, 158); // Blu più scuro per l'hover del main
    private static final Color DROPDOWN_TOGGLE_BG = new Color(0, 90, 158); // Sfondo Pulsante Toggle
    private static final Color DROPDOWN_TOGGLE_HOVER = new Color(0, 70, 125); // Hover Pulsante Toggle
    private static final Color BORDER_DARK = new Color(60, 60, 60);     // Bordo
    private static final Color TEXT_LIGHT = new Color(255, 255, 255);   // Testo Bianco

    public ModernSplitButton(JFrame parent, String mainLabel, FormatExtensionDTO[] formats) {
        // 1. Setup del Pannello Contenitore
        // Uso un BorderLayout: il pulsante principale a sinistra (WEST), il toggle a destra (EAST).
        // Il GridLayout(1, 2) funziona bene, ma BorderLayout è più flessibile per i pesi.
        setLayout(new BorderLayout(0, 0));

        // Imposta il bordo e gli angoli arrotondati (richiede implementazione custom per gli angoli)
        setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1));

        // 2. Pulsante Azione Principale (CONVERTI in ...)
        mainActionButton = new JButton(mainLabel);
        mainActionButton.setBackground(ACCENT_BLUE);
        mainActionButton.setForeground(TEXT_LIGHT);
        mainActionButton.setFont(mainActionButton.getFont().deriveFont(Font.BOLD, 14f));
        mainActionButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        mainActionButton.setFocusPainted(false);
        mainActionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Listener Hover per l'azione principale
        mainActionButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (mainActionButton.isEnabled()) {
                    mainActionButton.setBackground(ACCENT_BLUE_DARK);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                mainActionButton.setBackground(ACCENT_BLUE);
            }
        });

        // 3. Pulsante Toggle Dropdown (Freccia giù)
        dropdownToggleButton = new JButton("▼");
        dropdownToggleButton.setBackground(DROPDOWN_TOGGLE_BG);
        dropdownToggleButton.setForeground(TEXT_LIGHT);
        // Bordo separatore sottile
        dropdownToggleButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(255, 255, 255, 50)), // Linea sottile verticale
                new EmptyBorder(8, 10, 8, 10) // Padding interno
        ));
        dropdownToggleButton.setFocusPainted(false);
        dropdownToggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Listener Hover per il toggle
        dropdownToggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (dropdownToggleButton.isEnabled()) {
                    dropdownToggleButton.setBackground(DROPDOWN_TOGGLE_HOVER);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                dropdownToggleButton.setBackground(DROPDOWN_TOGGLE_BG);
            }
        });

        // 4. Inizializzazione del Menu a tendina
        formatMenu = new JPopupMenu();
        formatMenu.setBorder(BorderFactory.createLineBorder(BORDER_DARK)); // Bordo scuro

        // Personalizza il renderer per un aspetto Dark Mode coerente
        formatMenu.putClientProperty("JPopupMenu.showSeparator", true);

        // Aggiungi le voci del menu
        for (FormatExtensionDTO format : formats) {
            JMenuItem item = new JMenuItem(format.toString());
            // Simula lo stile del mockup (hover blu)
            item.setBackground(new Color(37, 37, 38)); // surface-dark
            item.setForeground(TEXT_LIGHT);

            item.addActionListener(e -> {
                // Esempio: aggiorna il testo del pulsante principale
                setMainActionLabel("CONVERTI in " + format);
                this.format = format;
                JOptionPane.showMessageDialog(this, "Formato selezionato: " + format, "Selezione", JOptionPane.INFORMATION_MESSAGE);
                parent.pack();
            });
            formatMenu.add(item);
        }

        // 5. Listener per il Pulsante Toggle
        dropdownToggleButton.addActionListener(e -> {
            // Mostra il menu sotto il pulsante
            formatMenu.show(dropdownToggleButton, 0, dropdownToggleButton.getHeight());
        });

        // 6. Aggiunta dei Componenti al Pannello
        add(mainActionButton, BorderLayout.CENTER);
        add(dropdownToggleButton, BorderLayout.EAST);
    }

    public FormatExtensionDTO getSelectedItem() {
        return format;
    }

    // Metodo per cambiare il testo dell'azione principale
    public void setMainActionLabel(String label) {
        mainActionButton.setText(label);
    }

    // Metodo per rendere l'intero componente non modificabile/disabilitato
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mainActionButton.setEnabled(enabled);
        dropdownToggleButton.setEnabled(enabled);

        // Modifica l'aspetto quando disabilitato
        if (!enabled) {
            mainActionButton.setBackground(new Color(60, 60, 60));
            dropdownToggleButton.setBackground(new Color(40, 40, 40));
            setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
        } else {
            mainActionButton.setBackground(ACCENT_BLUE);
            dropdownToggleButton.setBackground(DROPDOWN_TOGGLE_BG);
            setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1));
        }
    }

//    // Main per testing
//    public static void main(String[] args) {
//        // Applica un tema Dark per coerenza con il design
//        try {
//            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
//        } catch (Exception ex) {
//            System.err.println("Failed to initialize FlatDarkLaf");
//        }
//
//        SwingUtilities.invokeLater(() -> {
//            JFrame frame = new JFrame("Esempio Modern Split Button");
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.setSize(500, 300);
//            frame.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
//
//            String[] formats = {"PNG", "JPG", "WEBP", "PDF (Multi-pagina)"};
//
//            // 1. Pulsante Attivo
//            ModernSplitButton splitButton1 = new ModernSplitButton("CONVERTI in PNG", formats);
//            splitButton1.setMainActionLabel("CONVERTI in PNG");
//            splitButton1.mainActionButton.addActionListener(e -> {
//                JOptionPane.showMessageDialog(frame, "Azione principale eseguita: CONVERTI.", "Azione", JOptionPane.INFORMATION_MESSAGE);
//            });
//
//            // 2. Pulsante Disabilitato
//            ModernSplitButton splitButton2 = new ModernSplitButton("CONVERTI in JPG", formats);
//            splitButton2.setMainActionLabel("CONVERTI in JPG");
//            splitButton2.setEnabled(false);
//
//            frame.add(new JLabel("Pulsante Split Attivo:"));
//            frame.add(splitButton1);
//            frame.add(new JLabel("Pulsante Split Disabilitato:"));
//            frame.add(splitButton2);
//
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//        });
//    }
}

