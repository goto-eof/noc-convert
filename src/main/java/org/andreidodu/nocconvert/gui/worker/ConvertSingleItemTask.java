package org.andreidodu.nocconvert.gui.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ConvertSingleItemTask implements Runnable {
    private static final Logger log = LogManager.getLogger(ConvertSingleItemTask.class);
    private final Consumer<ConversionItemDTO> publish;
    private final ConversionItemDTO conversionItemDTO;
    private final ImageConverterService imageConverterService;
    private final Consumer<ConversionItemDTO> onItemCompleted;
    private LocalDateTime lastCall = LocalDateTime.now();
    private boolean canceled = false;
    private final Semaphore semaphore;
    private final ExecutorService platformExecutorService;

    public ConvertSingleItemTask(
            ConversionItemDTO conversionItemDTO,
            Consumer<ConversionItemDTO> publish,
            Consumer<ConversionItemDTO> onItemCompleted,
            Semaphore semaphore,
            ExecutorService platformExecutorService
    ) {
        this.conversionItemDTO = conversionItemDTO;
        this.publish = publish;
        this.platformExecutorService = platformExecutorService;
        this.imageConverterService = new ImageConverterServiceImpl(this.platformExecutorService);
        this.onItemCompleted = onItemCompleted;
        this.semaphore = semaphore;
    }

    private void onStart() {
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(0f);
        publish.accept(conversionItemDTO);
    }


    private void onProgress(Float progress) {
        if (progress < 1 && conversionItemDTO.getProgressPercentage() == 0) {
            progress = 1f;
        }
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(progress);
        publish.accept(conversionItemDTO);
    }

    public void run() {
        if (canceled) {
            return;
        }
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            return;
        }
        if (canceled) {
            return;
        }
        try {
            this.imageConverterService.convertImage(
                    conversionItemDTO.getSourceFile(),
                    conversionItemDTO.getDestinationDirectory(),
                    conversionItemDTO.getTargetExtension(),
                    this::onStart,
                    this::onProgressController
            );
            conversionItemDTO.setStatus(ConversionStatus.COMPLETED);
            onItemCompleted.accept(conversionItemDTO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            conversionItemDTO.setStatus(ConversionStatus.FAILED);
            conversionItemDTO.setProgressPercentage(100f);
            conversionItemDTO.setErrorMessage(getRootCauseMessage(e));
            publish.accept(conversionItemDTO);
            onItemCompleted.accept(conversionItemDTO);
        } finally {
            semaphore.release();
        }
    }

    private void onProgressController(float progress) {
        if ((progress == 1 || progress % 3 == 0) && Duration.between(lastCall, LocalDateTime.now()).toMillis() >= 1000) {
            this.onProgress(progress);
            lastCall = LocalDateTime.now();
        }
    }

    public void closeStreams() {
        this.canceled = true;
        imageConverterService.cancelTask();
    }
}
