package org.andreidodu.nocconvert.gui.controller.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class ConversionChildTask implements Runnable {
    private static final Log log = LogFactory.getLog(ConversionChildTask.class);
    private final ConversionItemDTO conversionDTO;
    private final ImageConverterService imageConverterService;
    private long progressIndex = 0;
    private LocalDateTime lastPublish = LocalDateTime.now();
    private final Consumer<ConversionItemDTO> publishOnParent;

    public ConversionChildTask(
            ConversionItemDTO conversionDTO,
            Consumer<ConversionItemDTO> publishOnParent
    ) {
        this.conversionDTO = conversionDTO;
        this.imageConverterService = new ImageConverterServiceImpl();
        this.publishOnParent = publishOnParent;
    }

    @Override
    public void run() {
        try {
            imageConverterService.convertImage(
                    conversionDTO.getSourceFile(),
                    conversionDTO.getDestinationDirectory(),
                    conversionDTO.getTargetExtension(),
                    this::onStart,
                    this::onProgress,
                    this::onDone
            );
        } catch (Exception e) {
            conversionDTO.setStatus(ConversionStatus.FAILED);
            conversionDTO.setErrorMessage(e.getMessage());
            publishOnParent.accept(conversionDTO);
            log.debug(e.getMessage(), e);
        }
    }

    protected void onDone() {
        conversionDTO.setProgressPercentage(100);
        conversionDTO.setStatus(ConversionStatus.COMPLETED);
        publishOnParent.accept(conversionDTO);
    }

    private void onStart() {
        conversionDTO.setProgressPercentage(0);
        conversionDTO.setStatus(ConversionStatus.PROCESSING);
        publishOnParent.accept(conversionDTO);
    }

    private void onProgress(Float progressValue) {
        if (progressIndex % 2 == 0 && Duration.between(lastPublish, LocalDateTime.now()).compareTo(Duration.ofMillis(10)) >= 0) {
            conversionDTO.setProgressPercentage(progressValue.intValue());
            publishOnParent.accept(conversionDTO);
            lastPublish = LocalDateTime.now();
        }

        if (Thread.currentThread().isInterrupted()) {
            conversionDTO.setStatus(ConversionStatus.CANCELED);
            publishOnParent.accept(conversionDTO);
            throw new RuntimeException("Interrupted");
        }
        progressIndex++;
    }

}
