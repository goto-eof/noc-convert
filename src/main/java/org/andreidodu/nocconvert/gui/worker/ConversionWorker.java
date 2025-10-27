package org.andreidodu.nocconvert.gui.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class ConversionWorker extends SwingWorker<Void, ConversionItemDTO> {
    private static final Logger log = LogManager.getLogger(ConversionWorker.class);
    private final List<ConversionItemDTO> list;
    private final Consumer<List<ConversionItemDTO>> updateGui;
    private final Runnable onDone;
    private final Runnable onItemCompleted;
    private ConversionOrchestrator conversionOrchestrator;

    public ConversionWorker(List<ConversionItemDTO> list, Consumer<List<ConversionItemDTO>> updateGui, Runnable onDone, Runnable onItemCompleted) {
        this.list = list;
        this.updateGui = updateGui;
        this.onDone = onDone;
        this.onItemCompleted = onItemCompleted;
    }

    @Override
    protected Void doInBackground() throws Exception {
        conversionOrchestrator = new ConversionOrchestrator(list, this::publishItem, onItemCompleted);
        return null;
    }


    private void publishItem(ConversionItemDTO conversionItemDTO) {
        publish(conversionItemDTO);
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            log.debug("Conversion Worker cancelled.");
        }
        this.onDone.run();
    }


    public void shutdown(boolean shutdown) {
        if (conversionOrchestrator != null) {
            conversionOrchestrator.cancel();
        }
        this.cancel(shutdown);
    }
}
