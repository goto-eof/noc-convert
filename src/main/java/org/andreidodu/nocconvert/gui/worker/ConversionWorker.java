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
    private final Runnable onConversionDone;
    private final Consumer<ConversionItemDTO> onItemCompleted;
    private ConversionOrchestrator conversionOrchestrator;
    private final Runnable onAllTasksComplete;

    public ConversionWorker(List<ConversionItemDTO> list, Consumer<List<ConversionItemDTO>> updateGui, Runnable onConversionDone, Consumer<ConversionItemDTO> onItemCompleted, Runnable onAllTasksComplete) {
        this.list = list;
        this.updateGui = updateGui;
        this.onConversionDone = onConversionDone;
        this.onItemCompleted = onItemCompleted;
        this.onAllTasksComplete = onAllTasksComplete;
    }

    @Override
    protected Void doInBackground() {
        conversionOrchestrator = new ConversionOrchestrator(list, this::publishItem, onItemCompleted, onAllTasksComplete);
        conversionOrchestrator.startConversion();
        return null;
    }

    public int getVirtualThreadsPermits() {
        return conversionOrchestrator.getVirtualThreadsPermits();
    }


    public int getPlatformThreadsPermits() {
        return conversionOrchestrator.getPlatformThreadsPermits();
    }

    private void cancelOrchestratorJob() {
        if (conversionOrchestrator != null) {
            conversionOrchestrator.shutdown();
        }
    }

    @Override
    protected void process(List<ConversionItemDTO> chunks) {
        this.updateGui.accept(chunks);
    }

    private void publishItem(ConversionItemDTO conversionItemDTO) {
        publish(conversionItemDTO);
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            log.debug("Conversion Worker cancelled.");
        }
        this.onConversionDone.run();
    }

    public void shutdown(boolean shutdown) {
        cancelOrchestratorJob();
        this.cancel(shutdown);
        this.onConversionDone.run();
    }

}
