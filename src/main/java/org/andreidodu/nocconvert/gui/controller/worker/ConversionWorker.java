package org.andreidodu.nocconvert.gui.controller.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class ConversionWorker extends SwingWorker<Void, ConversionItemDTO> {
    private final List<ConversionItemDTO> list;
    private final Consumer<List<ConversionItemDTO>> updateGui;
    private final Runnable onDone;
    private final Runnable onItemCompleted;

    public ConversionWorker(List<ConversionItemDTO> list, Consumer<List<ConversionItemDTO>> updateGui, Runnable onDone, Runnable onItemCompleted) {
        this.list = list;
        this.updateGui = updateGui;
        this.onDone = onDone;
        this.onItemCompleted = onItemCompleted;
    }

    @Override
    protected Void doInBackground() throws Exception {
        new ConversionOrchestrator(list, this::publishItem,onItemCompleted );
        return null;
    }

    private void publishItem(ConversionItemDTO conversionItemDTO) {
        //this.updateGui.accept(List.of(conversionItemDTO));
        publish(conversionItemDTO);
    }

    @Override
    protected void process(List<ConversionItemDTO> chunks) {

    }

    @Override
    protected void done() {
        this.onDone.run();
    }
}
