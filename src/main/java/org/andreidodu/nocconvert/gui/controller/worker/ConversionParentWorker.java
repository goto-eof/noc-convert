package org.andreidodu.nocconvert.gui.controller.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.util.CustomThreadFactory;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversionParentWorker extends SwingWorker<Void, ConversionItemDTO> {
    private final List<ConversionItemDTO> conversionItemList;
    private final JList<ConversionItemDTO> conversionItemJList;
    private ExecutorService conversionExecutorPoolService;

    public ConversionParentWorker(List<ConversionItemDTO> conversionItemList, JList<ConversionItemDTO> conversionItemJList) {
        this.conversionItemList = conversionItemList;
        this.conversionItemJList = conversionItemJList;
    }

    @Override
    protected Void doInBackground() {

        int numberOfProcessors = Runtime.getRuntime().availableProcessors();
        try (ExecutorService conversionExecutorPoolService = Executors.newFixedThreadPool(numberOfProcessors, new CustomThreadFactory())) {
            this.conversionExecutorPoolService = conversionExecutorPoolService;

            for (ConversionItemDTO dto : conversionItemList) {
                conversionExecutorPoolService.execute(() -> {
                    new ConversionChildTask(dto, this::publishOnParent);
                });
            }
        }
        return null;
    }

    @Override
    protected void process(List<ConversionItemDTO> chunks) {
        DefaultListModel<ConversionItemDTO> model = (DefaultListModel<ConversionItemDTO>) conversionItemJList.getModel();

        chunks.forEach(conversionItem -> {
            model.setElementAt(conversionItem, conversionItem.getIndex());
        });

    }

    private synchronized void stopConversion() {
        if (conversionExecutorPoolService != null && !conversionExecutorPoolService.isTerminated()) {
            conversionExecutorPoolService.shutdownNow();
        }
    }

    private void publishOnParent(ConversionItemDTO dto) {
        publish(dto);
    }
}
