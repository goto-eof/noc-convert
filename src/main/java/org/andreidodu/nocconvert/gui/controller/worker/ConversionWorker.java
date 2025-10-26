package org.andreidodu.nocconvert.gui.controller.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class ConversionWorker extends SwingWorker<Void, ConversionItemDTO> {
    private final List<ConversionItemDTO> list;
    private final Consumer<List<ConversionItemDTO>> updateGui;

    public ConversionWorker(List<ConversionItemDTO> list, Consumer<List<ConversionItemDTO>> updateGui) {
        this.list = list;
        this.updateGui = updateGui;
    }

    @Override
    protected Void doInBackground() throws Exception {
        new ConversionOrchestrator(list, this::publishItem);
        return null;
    }

    private void publishItem(ConversionItemDTO conversionItemDTO) {
        this.updateGui.accept(List.of(conversionItemDTO));
    }

    @Override
    protected void process(List<ConversionItemDTO> chunks) {
        updateGui.accept(chunks);
    }

    @Override
    protected void done() {
    }
}
