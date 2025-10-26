package org.andreidodu.nocconvert.gui.controller.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.gui.controller.ConversionController;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.Consumer;

public class ConvertSingleItemTask implements Runnable {
    private static final Logger log = LogManager.getLogger(ConversionController.class);
    private final Consumer<ConversionItemDTO> publish;
    private final ConversionItemDTO conversionItemDTO;
    private final ImageConverterService imageConverterService;

    public ConvertSingleItemTask(
            ConversionItemDTO conversionItemDTO,
            Consumer<ConversionItemDTO> publish
    ) {
        this.conversionItemDTO = conversionItemDTO;
        this.publish = publish;
        this.imageConverterService = new ImageConverterServiceImpl();
    }

    private void onStart() {
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(0);
        publish.accept(conversionItemDTO);
    }


    private void onProgress(Float progress) {
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(progress.intValue());
        publish.accept(conversionItemDTO);
    }

    private void onDone() {
        conversionItemDTO.setStatus(ConversionStatus.COMPLETED);
        conversionItemDTO.setProgressPercentage(100);
        publish.accept(conversionItemDTO);
    }

    public void run() {
        try {
            this.imageConverterService.convertImage(
                    conversionItemDTO.getSourceFile(),
                    conversionItemDTO.getDestinationDirectory(),
                    conversionItemDTO.getTargetExtension(),
                    this::onStart,
                    this::onProgress,
                    this::onDone);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            conversionItemDTO.setStatus(ConversionStatus.FAILED);
            conversionItemDTO.setProgressPercentage(0);
            publish.accept(conversionItemDTO);
        }
    }
}
