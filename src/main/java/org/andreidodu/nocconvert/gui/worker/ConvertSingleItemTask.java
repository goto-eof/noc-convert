package org.andreidodu.nocconvert.gui.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ConvertSingleItemTask implements Runnable {
    private static final Logger log = LogManager.getLogger(ConvertSingleItemTask.class);
    private final Consumer<ConversionItemDTO> publishItemUpdate;
    private final ConversionItemDTO conversionItemDTO;
    private final ImageConverterService imageConverterService;
    private final Consumer<ConversionItemDTO> setItemAsCompleted;
    private final LocalDateTime lastCall = LocalDateTime.now();
    private boolean canceled = false;
    private final Semaphore semaphore;
    private final ExecutorService platformExecutorService;

    public ConvertSingleItemTask(
            ConversionItemDTO conversionItemDTO,
            Consumer<ConversionItemDTO> publishItemUpdate,
            Consumer<ConversionItemDTO> setItemAsCompleted,
            Semaphore semaphore,
            ExecutorService platformExecutorService
    ) {
        this.conversionItemDTO = conversionItemDTO;
        this.publishItemUpdate = publishItemUpdate;
        this.platformExecutorService = platformExecutorService;
        this.imageConverterService = new ImageConverterServiceImpl(this.platformExecutorService);
        this.setItemAsCompleted = setItemAsCompleted;
        this.semaphore = semaphore;
    }

    private void onStart() {
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(0f);
        publishItemUpdate.accept(conversionItemDTO);
    }


//    private void updateProgressValue(Float progress) {
//        if (progress < 1 && conversionItemDTO.getProgressPercentage() == 0) {
//            progress = 1f;
//        }
//        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
//        conversionItemDTO.setProgressPercentage(progress);
//        publishItemUpdate.accept(conversionItemDTO);
//    }

    public void run() {
        if (canceled) {
            updateAsCancelled();
            return;
        }
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            updateAsCancelled();
            return;
        }
        if (canceled) {
            updateAsCancelled();
            return;
        }
        try {
            this.imageConverterService.convertImage(
                    conversionItemDTO.getSourceFile(),
                    conversionItemDTO.getDestinationDirectory(),
                    conversionItemDTO.getTargetExtension(),
                    this::onStart,
                    this::updateProgressFloatValue
            );
            conversionItemDTO.setStatus(ConversionStatus.COMPLETED);
            conversionItemDTO.setProgressPercentage(100f);
            setItemAsCompleted.accept(conversionItemDTO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            conversionItemDTO.setStatus(ConversionStatus.FAILED);
            conversionItemDTO.setProgressPercentage(100f);
            conversionItemDTO.setErrorMessage(getRootCauseMessage(e));
            setItemAsCompleted.accept(conversionItemDTO);
        } finally {
            semaphore.release();
        }
    }

    private void updateAsCancelled() {
        conversionItemDTO.setStatus(ConversionStatus.FAILED);
        conversionItemDTO.setProgressPercentage(100f);
        conversionItemDTO.setErrorMessage("cancelled");
        setItemAsCompleted.accept(conversionItemDTO);
    }

    private void updateProgressFloatValue(float progress) {
        // if ((progress == 1 || progress % 3 == 0) && Duration.between(lastCall, LocalDateTime.now()).toMillis() >= 1000) {
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(progress);
//        lastCall = LocalDateTime.now();
        publishItemUpdate.accept(conversionItemDTO);
        //}
    }

    public void closeStreams() {
        this.canceled = true;
        imageConverterService.cancelTask();
    }
}
