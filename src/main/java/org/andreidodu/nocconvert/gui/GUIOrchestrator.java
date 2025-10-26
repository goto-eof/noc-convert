package org.andreidodu.nocconvert.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.components.ListItemComponent;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.components.TextfieldButtonComponent;
import org.andreidodu.nocconvert.gui.constants.Colors;
import org.andreidodu.nocconvert.gui.controller.ConversionController;
import org.andreidodu.nocconvert.gui.controller.ConvertionStatusController;
import org.andreidodu.nocconvert.gui.controller.PathSelectionController;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.ConvertionStatusDTO;
import org.andreidodu.nocconvert.gui.dto.PathSelectionDTO;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;

public class GUIOrchestrator extends JFrame {
    private static final float ARC_SIZE = 20;
    @Getter
    private JPanel mainPanel;
    private TextfieldButtonComponent sourceComponent;
    private JList<ConversionItemDTO> conversionFileList;
    private TextfieldButtonComponent destinationComponent;
    private SplitButtonComponent convertComponent;
    private JLabel fileFormatsJLabel;
    private JPanel sourcePanel;
    private JPanel destinationPanel;
    private JPanel directoriesPanel;
    private JProgressBar progressBar1;
    private JButton button1;
    private JScrollPane scrollPane1;
    private JPanel headerPanel;
    private JPanel footerPanel;
    private JPanel scrollPanePanel;
    private JLabel applicationStatusLabel;
    private JPanel progressBarPanel;

    private final PathSelectionController pathSelectionController;
    private final ConvertionStatusController convertionStatusController;
    private final ConversionController conversionController;


    public GUIOrchestrator() {
        super();
        preInitialization();
        $$$setupUI$$$();

        // START TODO move somewhere else
        buildModelForTargetFileFormat();
        applicationStatusLabel.setVisible(true);
        progressBar1.setForeground(Colors.LIME_DARK);
        progressBarPanel.setVisible(true);
        // END TODO move somewhere else

        pathSelectionController = initializePathSelectionController();
        convertionStatusController = initializeProcessingStatusController();
        conversionController = initializeConversionController();

        postInitialization();
    }

    private void postInitialization() {
        initializeWindow();
    }

    private void preInitialization() {
        convertComponent = new SplitButtonComponent(this);
    }

    private PathSelectionController initializePathSelectionController() {
        PathSelectionDTO pathSelectionDTO = PathSelectionDTO.builder()
                .guiOrchestrator(this)
                .sourceComponent(sourceComponent)
                .destinationComponent(destinationComponent)
                .build();
        return new PathSelectionController(pathSelectionDTO);
    }


    private ConvertionStatusController initializeProcessingStatusController() {
        ConvertionStatusDTO convertionStatusDTO = new ConvertionStatusDTO();
        return new ConvertionStatusController(convertionStatusDTO);
    }

    public Path getSourceDirectory() {
        return pathSelectionController.getPathSelectionRawDTO().getSourceDirectory();
    }

    public Path getDestinationDirectory() {
        return pathSelectionController.getPathSelectionRawDTO().getDestinationDirectory();
    }

    public void updateApplicationStatusLabel(String text) {
        applicationStatusLabel.setText(text);
    }

    private ConversionController initializeConversionController() {
        ConversionDTO conversionDTO = ConversionDTO.builder()
                .guiOrchestrator(this)
                .convertComponent(convertComponent)
                .applicationStatusLabel(applicationStatusLabel)
                .build();
        return new ConversionController(conversionDTO);
    }


    private void initializeWindow() {
        setTitle("NoCloud Bulk Image Converter");
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        revalidate();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public List<String> getValidationMessageList() {
        return pathSelectionController.retrieveValidationErrorsIfExists();
    }

    /*==========================================================================================*/
    // START of Old code that should be refactored
    /*==========================================================================================*/

    private SplitButtonComponent buildModelForTargetFileFormat() {


        DefaultListModel<ConversionItemDTO> modelProgress = new DefaultListModel<>();
        ListItemComponent renderer = new ListItemComponent();

        Random random = new Random();
        IntStream.range(0, 100).forEach(i -> {
            ConversionItemDTO conversionItemDTO = new ConversionItemDTO();
            conversionItemDTO.setTargetFormat("PNG");
            int num = random.nextInt(100000000);
            conversionItemDTO.setFileName("file" + num + ".png");
            conversionItemDTO.setStatus(new ConversionStatus("PROCESSING", Color.GREEN, "BOOM"));
            int percentage = random.nextInt(100);
            conversionItemDTO.setProgressPercentage(percentage);
            int size = random.nextInt(2048);
            conversionItemDTO.setFileSize(size);
            modelProgress.addElement(conversionItemDTO);
        });


        conversionFileList = new JList<>(modelProgress);
        conversionFileList.setCellRenderer(renderer);
//        conversionFileList.setBorder(BorderFactory.createLineBorder(LIST_BG, 1));


        return convertComponent;
    }


    /** Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBackground(new Color(-14079186));
        mainPanel.setPreferredSize(new Dimension(800, 600));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setBackground(new Color(-13815754));
        mainPanel.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel2.setBackground(new Color(-14079186));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        directoriesPanel = new JPanel();
        directoriesPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        directoriesPanel.setBackground(new Color(-14079186));
        panel2.add(directoriesPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sourcePanel = new JPanel();
        sourcePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 10, 0), -1, -1));
        sourcePanel.setBackground(new Color(-14079186));
        directoriesPanel.add(sourcePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.setBackground(new Color(-14079186));
        sourcePanel.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.add(sourceComponent, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        destinationPanel = new JPanel();
        destinationPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        destinationPanel.setBackground(new Color(-14079186));
        directoriesPanel.add(destinationPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.setBackground(new Color(-14079186));
        destinationPanel.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.add(destinationComponent, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        fileFormatsJLabel = new JLabel();
        fileFormatsJLabel.setEnabled(true);
        fileFormatsJLabel.setForeground(new Color(-8289660));
        fileFormatsJLabel.setHorizontalAlignment(2);
        fileFormatsJLabel.setText("Loading file formats...");
        fileFormatsJLabel.setVisible(false);
        fileFormatsJLabel.putClientProperty("html.disable", Boolean.FALSE);
        mainPanel.add(fileFormatsJLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        footerPanel.setLayout(new GridLayoutManager(2, 3, new Insets(10, 10, 10, 10), -1, -1));
        footerPanel.setBackground(new Color(-13026240));
        footerPanel.setOpaque(true);
        mainPanel.add(footerPanel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        footerPanel.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        convertComponent.setBackground(new Color(-13026240));
        convertComponent.setOpaque(false);
        footerPanel.add(convertComponent, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.setBackground(new Color(-13026240));
        footerPanel.add(panel5, new GridConstraints(0, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, Font.BOLD, 14, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setForeground(new Color(-8289660));
        label1.setText("noc-convert v.2.0.0");
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, -1, 12, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setForeground(new Color(-8421246));
        label2.setText("by Andrei Dodu");
        panel5.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel6.setBackground(new Color(-14079186));
        mainPanel.add(panel6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPanePanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPanePanel.setBackground(new Color(-13026240));
        scrollPanePanel.setEnabled(true);
        scrollPanePanel.setOpaque(false);
        panel6.add(scrollPanePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane1.setBackground(new Color(-14079186));
        scrollPane1.setForeground(new Color(-2038305));
        scrollPane1.setOpaque(false);
        scrollPanePanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        conversionFileList.setBackground(new Color(-14079186));
        conversionFileList.setOpaque(true);
        conversionFileList.setRequestFocusEnabled(false);
        conversionFileList.setSelectionBackground(new Color(-16756358));
        conversionFileList.setSelectionForeground(new Color(-8026747));
        scrollPane1.setViewportView(conversionFileList);
        applicationStatusLabel = new JLabel();
        applicationStatusLabel.setText("Please select the Source and the Destination Directories and press on the Convert button.");
        scrollPanePanel.add(applicationStatusLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new GridLayoutManager(2, 2, new Insets(10, 20, 10, 20), -1, -1));
        progressBarPanel.setBackground(new Color(-14079186));
        mainPanel.add(progressBarPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, Font.BOLD, -1, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setText("Total Progress");
        progressBarPanel.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressBar1 = new JProgressBar();
        progressBar1.setValue(55);
        progressBarPanel.add(progressBar1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        Font panel7Font = this.$$$getFont$$$(null, Font.BOLD, 9, panel7.getFont());
        if (panel7Font != null) panel7.setFont(panel7Font);
        panel7.setOpaque(false);
        progressBarPanel.add(panel7, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setBackground(new Color(-14079186));
        Font label4Font = this.$$$getFont$$$(null, Font.BOLD, 12, label4.getFont());
        if (label4Font != null) label4.setFont(label4Font);
        label4.setText("Conversion Complete: 6 files converted / 1 error");
        panel7.add(label4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel7.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        headerPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 5, 0, 5), -1, -1));
        headerPanel.setBackground(new Color(-13026240));
        mainPanel.add(headerPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button1 = new JButton();
        button1.setBackground(new Color(-13026240));
        button1.setBorderPainted(false);
        button1.setFocusable(false);
        button1.setForeground(new Color(-14079186));
        button1.setIcon(new ImageIcon(getClass().getResource("/images/hamburger.png")));
        button1.setText("");
        headerPanel.add(button1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(48, 48), new Dimension(48, 48), new Dimension(48, 48), 0, false));
        final JLabel label5 = new JLabel();
        label5.setBackground(new Color(-14079186));
        Font label5Font = this.$$$getFont$$$(null, Font.BOLD, 20, label5.getFont());
        if (label5Font != null) label5.setFont(label5Font);
        label5.setText("NoCloud Bulk Image Converter");
        headerPanel.add(label5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        headerPanel.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /** @noinspection ALL */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /** @noinspection ALL */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }


    private void createUIComponents() {
        convertComponent = buildModelForTargetFileFormat();
        sourceComponent = new TextfieldButtonComponent("", "Source");
        destinationComponent = new TextfieldButtonComponent("", "Destination");

        headerPanel = new JPanel();
        Color headerBorderColor = new Color(168, 172, 174, 255);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, headerBorderColor));


        footerPanel = new JPanel(new BorderLayout(0, 0));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, headerBorderColor));

        scrollPane1 = new JScrollPane();
        scrollPane1.setVisible(false);
        scrollPane1.setViewportView(conversionFileList);
        scrollPane1.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));


        scrollPanePanel = new JPanel() {
            {
                setOpaque(false);

            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getWidth();
                int height = getHeight();
                g2.setColor(new Color(57, 60, 64));
                g2.fill(new RoundRectangle2D.Float(
                        0, 0, width, height, ARC_SIZE, ARC_SIZE
                ));

                g2.dispose();

            }

        };

    }

}
