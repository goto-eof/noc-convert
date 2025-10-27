package org.andreidodu.nocconvert.gui.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class ConvertSingleItemTask implements Runnable {
    private static final Logger log = LogManager.getLogger(ConvertSingleItemTask.class);
    private final Consumer<ConversionItemDTO> publish;
    private final ConversionItemDTO conversionItemDTO;
    private final ImageConverterService imageConverterService;
    private final Runnable onItemCompleted;
    private LocalDateTime lastCall = LocalDateTime.now();
    private boolean canceled = false;
    private final Semaphore semaphore;

    public ConvertSingleItemTask(
            ConversionItemDTO conversionItemDTO,
            Consumer<ConversionItemDTO> publish,
            Runnable onItemCompleted,
            Semaphore semaphore
    ) {
        this.conversionItemDTO = conversionItemDTO;
        this.publish = publish;
        this.imageConverterService = new ImageConverterServiceImpl();
        this.onItemCompleted = onItemCompleted;
        this.semaphore = semaphore;
    }

    private void onStart() {
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(0);
        publish.accept(conversionItemDTO);
    }


    private void onProgress(Float progress) {
        if (progress < 1 && conversionItemDTO.getProgressPercentage() == 0) {
            progress = 1f;
        }
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(progress.intValue());
        publish.accept(conversionItemDTO);
    }

    private void onDone() {
        conversionItemDTO.setStatus(ConversionStatus.COMPLETED);
        conversionItemDTO.setProgressPercentage(100);
        publish.accept(conversionItemDTO);
        onItemCompleted.run();
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
                    this::onProgressController,
                    this::onDone);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            conversionItemDTO.setStatus(ConversionStatus.FAILED);
            conversionItemDTO.setProgressPercentage(100);
            publish.accept(conversionItemDTO);
            onItemCompleted.run();
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
        imageConverterService.closeAllStreams();
    }
}
