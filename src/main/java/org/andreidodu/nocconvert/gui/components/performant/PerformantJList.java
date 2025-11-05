package org.andreidodu.nocconvert.gui.components.performant;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;

public class PerformantJList extends JList<ConversionItemDTO> {
    private static final Logger log = LogManager.getLogger(PerformantJList.class);

    private final int rowHeight;

    private static final int PREFERRED_VISIBLE_ROWS = 10;

    public PerformantJList(ListModel<ConversionItemDTO> model) {
        super(model);
        PerformantCellRenderer cellRenderer = new PerformantCellRenderer();
        setCellRenderer(cellRenderer);
        rowHeight = cellRenderer.getRowHeight();
        setFixedCellHeight(rowHeight);

        if (model instanceof AbstractListModel) {
            model.addListDataListener(new ListDataListener() {

                @Override
                public void intervalAdded(ListDataEvent e) {
                    revalidate();
                }

                @Override
                public void intervalRemoved(ListDataEvent e) {
                    revalidate();
                }

                @Override
                public void contentsChanged(ListDataEvent e) {
                    repaint();
                }
            });
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int height = Math.min(rowHeight * getModel().getSize(), Integer.MAX_VALUE);
        return new Dimension(super.getPreferredSize().width, height);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        int rows = Math.min(PREFERRED_VISIBLE_ROWS, getModel().getSize());
        int height = rows * rowHeight;
        int width = super.getPreferredScrollableViewportSize().width;
        return new Dimension(width, height);
    }
}