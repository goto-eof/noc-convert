package org.andreidodu.nocconvert.gui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Questa classe crea un pannello Swing moderno che unisce un JTextField
 * e un JButton (Sfoglia) con un aspetto coeso, come nel mockup Dark Mode.
 */
public class ModernInputButtonPanel extends JPanel {

    private final JTextField textField;
    private final JButton browseButton;

    // Colori e stili (Adattati dalla palette Dark Mode)
    private static final Color LIST_BG = new Color(45, 45, 45);         // Sfondo Campo di testo
    private static final Color BUTTON_BG = new Color(56, 56, 56);       // Sfondo Pulsante "Sfoglia"
    private static final Color BORDER_DARK = new Color(60, 60, 60);     // Bordo normale
    private static final Color ACCENT_BLUE = new Color(0, 120, 212);    // Blu Accent (per il focus)
    private static final Color TEXT_LIGHT = new Color(212, 212, 212);   // Testo
    private static final int BORDER_RADIUS = 6;

    public ModernInputButtonPanel(String initialText) {
        // 1. Imposta il layout
        // Uso un BorderLayout: il campo di testo occupa il CENTRO (espande), il pulsante l'EST (lato destro)
        setLayout(new BorderLayout(0, 0));

        // 2. Styling del Pannello Contenitore (simula il border-group nel CSS)
        // Questo è il pannello che riceverà il bordo e l'effetto focus
        setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));

        // Imposta gli angoli arrotondati creando un bordo composito (richiede di sovrascrivere paintComponent per i clipping)
        // Per semplicità, ci affidiamo al JPanel come contenitore per il bordo principale
        setOpaque(false); // Il pannello di default non si disegna

        // 3. Inizializzazione del Campo di Testo
        textField = new JTextField(initialText);
        textField.setBackground(LIST_BG);
        textField.setForeground(TEXT_LIGHT);
        textField.setCaretColor(TEXT_LIGHT); // Colore cursore

        // PASSO CHIAVE: Rimuovi il bordo predefinito del JTextField
        textField.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0)); // Padding interno

        // Rimuovi lo stile di focus predefinito del JTextField (gestito dal pannello genitore)
        textField.setFocusable(false);

        // 4. Inizializzazione del Pulsante
        browseButton = new JButton("Sfoglia");
        browseButton.setBackground(BUTTON_BG);
        browseButton.setForeground(TEXT_LIGHT);

        // PASSO CHIAVE: Rimuovi il bordo predefinito del JButton
        browseButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        browseButton.setMargin(new Insets(0, 0, 0, 0));

        // 5. Aggiunta dei Componenti
        add(textField, BorderLayout.CENTER);
        add(browseButton, BorderLayout.EAST);

        // 6. Gestione del Focus e Hover (per l'effetto "moderno")

        // Aggiunge un listener di Focus al campo di testo
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                // Quando ottiene il focus, cambia il bordo del pannello
                setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 1));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                // Quando perde il focus, ripristina il bordo scuro
                setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
            }
        });

        // Aggiunge l'effetto hover al pulsante
        browseButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                // Cambia colore al passaggio del mouse
                if (browseButton.isEnabled()) {
                    browseButton.setBackground(new Color(76, 76, 76)); // Grigio più chiaro
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                // Ripristina il colore
                browseButton.setBackground(BUTTON_BG);
            }
        });

        // Abilita/Disabilita lo stato per coerenza (simula lo stato nei mockup)
        setEnabled(true);
    }

    // Metodo per rendere l'intero componente non modificabile/disabilitato
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        browseButton.setEnabled(enabled);

        // Se disabilitato, opacità per i pulsanti/input
        Color disabledColor = enabled ? TEXT_LIGHT : new Color(150, 150, 150);
        textField.setForeground(disabledColor);
        browseButton.setForeground(disabledColor);

        // Togli l'effetto focus se disabilitato
        if (!enabled) {
            setBorder(BorderFactory.createLineBorder(BORDER_DARK, 1));
        }
    }

    public static void main(String[] args) {
        // Avvia l'interfaccia sulla coda degli eventi di AWT (obbligatorio in Swing)
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Esempio Modern Input Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 250);
            frame.setLayout(new GridLayout(4, 1, 10, 10));
            frame.getContentPane().setBackground(new Color(30, 30, 30)); // Sfondo Dark Mode

            // Titolo
            JLabel title = new JLabel("Mockup Input Directory (Swing)");
            title.setForeground(TEXT_LIGHT);
            title.setHorizontalAlignment(SwingConstants.CENTER);
            frame.add(title);

            // 1. Input attivo
            ModernInputButtonPanel inputPanel1 = new ModernInputButtonPanel("C:\\Input\\Directory\\Attiva");
            frame.add(inputPanel1);

            // 2. Input disabilitato
            ModernInputButtonPanel inputPanel2 = new ModernInputButtonPanel("C:\\Output\\Directory\\Disabilitata");
            inputPanel2.setEnabled(false);
            frame.add(inputPanel2);

            // Pulsante di prova per mostrare il focus
            JButton testButton = new JButton("Clicca qui per perdere il Focus");
            testButton.setBackground(new Color(85, 85, 85));
            testButton.setForeground(TEXT_LIGHT);
            frame.add(testButton);

            frame.setLocationRelativeTo(null); // Centra la finestra
            frame.setVisible(true);
        });
    }
}
