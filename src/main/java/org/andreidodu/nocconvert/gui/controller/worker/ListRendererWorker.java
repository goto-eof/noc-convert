package org.andreidodu.nocconvert.gui.controller.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
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
        timer = new Timer(DELAY, e -> {
            long startTime = System.nanoTime();
            List<ConversionItemDTO> chunk = formatExtensionList.subList(pos, Integer.min(pos + BATCH_SIZE, formatExtensionList.size()));
            DefaultListModel<ConversionItemDTO> model = (DefaultListModel<ConversionItemDTO>) list.getModel();
            model.addAll(chunk);
            chunk.forEach(dto -> dto.setIndex(model.indexOf(dto)));

            long endTime = System.nanoTime();
            long durationNs = endTime - startTime;
            double durationMs = durationNs / 1_000_000.0;

            log.debug("EDT blocking time for the rendering of {} records: {} ms", BATCH_SIZE, durationMs);

            pos += BATCH_SIZE;
            if (timer != null && pos >= formatExtensionList.size()) {
                onFinish.run();
                timer.stop();
            }
        });

        timer.start();
    }
}
