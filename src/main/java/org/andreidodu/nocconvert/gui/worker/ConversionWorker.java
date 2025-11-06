package org.andreidodu.nocconvert.gui.worker;

import lombok.Getter;
import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionOrchestratorInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConversionWorkerInputDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.List;

public class ConversionWorker extends SwingWorker<Void, ConversionItemDTO> {
    private static final Logger log = LogManager.getLogger(ConversionWorker.class);
    @Getter
    private ConversionOrchestrator conversionOrchestrator;
    private final ConversionWorkerInputDTO conversionWorkerInputDTO;

    public ConversionWorker(ConversionWorkerInputDTO conversionWorkerInputDTO) {
        this.conversionWorkerInputDTO = conversionWorkerInputDTO;
    }

    @Override
    protected Void doInBackground() {
        ConversionOrchestratorInputDTO conversionOrchestratorInputDTO = buildInput();
        conversionOrchestrator = new ConversionOrchestrator(conversionOrchestratorInputDTO);
        conversionOrchestrator.startConversion();
        return null;
    }

    private ConversionOrchestratorInputDTO buildInput() {
        return ConversionOrchestratorInputDTO.builder()
                .conversionItemDTOList(conversionWorkerInputDTO.list())
                .setItemAsCompleted(conversionWorkerInputDTO.completeItem())
                .onAllTasksCompleteEnableComponents(conversionWorkerInputDTO.onAllTasksComplete())
                .publishItemUpdate(this::publishItemUpdate)
                .incrementFailures(conversionWorkerInputDTO.incrementFailures())
                .incrementPasses(conversionWorkerInputDTO.incrementPasses())
                .build();
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
        conversionWorkerInputDTO.updateGui().accept(chunks);
    }

    private void publishItemUpdate(ConversionItemDTO conversionItemDTO) {
        publish(conversionItemDTO);
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            log.debug("Conversion Worker cancelled.");
        }
        this.conversionOrchestrator.onAllTasksCompleteEnableComponents();
    }

    public Void shutdownThreads() {
        cancelOrchestratorJob();
        conversionOrchestrator.onAllTasksCompleteEnableComponents();
        cancel(true);
        return null;
    }

}
