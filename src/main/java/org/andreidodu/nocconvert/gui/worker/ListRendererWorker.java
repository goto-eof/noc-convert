package org.andreidodu.nocconvert.gui.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.gui.components.performant.PerformantListModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.List;

public class ListRendererWorker extends SwingWorker<List<ConversionItemDTO>, Void> {
    private static final Logger log = LogManager.getLogger(ListRendererWorker.class);
    private final List<ConversionItemDTO> formatExtensionList;
    private final JList<ConversionItemDTO> list;
    private final int BATCH_SIZE = 500;
    private final int DELAY = 10;
    private int pos;
    private Timer timer;
    private final Runnable onFinish;

    public ListRendererWorker(List<ConversionItemDTO> formatExtensionList, JList<ConversionItemDTO> list, Runnable onFinish) {
        this.formatExtensionList = formatExtensionList;
        this.list = list;
        this.onFinish = onFinish;
    }

    @Override
    protected List<ConversionItemDTO> doInBackground() throws Exception {
        return formatExtensionList;
    }

    @Override
    protected void done() {
        populateListGradually();
    }

    private void populateListGradually() {
        PerformantListModel model = (PerformantListModel) list.getModel();
        model.addAll(formatExtensionList);
        onFinish.run();
    }
}
