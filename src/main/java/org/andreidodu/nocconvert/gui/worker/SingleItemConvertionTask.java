package org.andreidodu.nocconvert.gui.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertSingleItemTaskInputDTO;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.mapper.ConversionItemDTOMapper;
import org.andreidodu.nocconvert.task.SingleImageConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class SingleItemConvertionTask implements Runnable {
    private static final Logger log = LogManager.getLogger(SingleItemConvertionTask.class);
    private final ConversionItemDTO conversionItemDTO;
    private final SingleImageConverter singleImageConverter;
    private volatile boolean canceled = false;
    private final ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO;
    LocalDateTime lastCall = LocalDateTime.now();

    public SingleItemConvertionTask(ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO) {
        this.convertSingleItemTaskInputDTO = convertSingleItemTaskInputDTO;
        this.conversionItemDTO = new ConversionItemDTOMapper().clone(convertSingleItemTaskInputDTO.conversionItemDTO());
        this.singleImageConverter = new SingleImageConverter(convertSingleItemTaskInputDTO.platformExecutorService());
    }

    private void onStart() {
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(0f);
        convertSingleItemTaskInputDTO.publishItemUpdate().accept(conversionItemDTO);
    }

    public void run() {
        if (canceled || Thread.currentThread().isInterrupted() || convertSingleItemTaskInputDTO.isParentInterrupted().get()) {
            cancelOperation();
            log.debug("thread is cancelled");
            return;
        }
        try {
            convertSingleItemTaskInputDTO.semaphore().acquire();
        } catch (InterruptedException e) {
            cancelOperation();
            log.debug("operation aborted");
            return;
        }
        try {
            ConvertImageInputDTO convertImageInputDTO = buildInput();
            this.singleImageConverter.convertImage(convertImageInputDTO);
        } catch (ConversionManualAbortedException e) {
            cancelOperation();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            convertSingleItemTaskInputDTO.semaphore().release();
        }
    }

    private void cancelOperation() {
        updateAsCancelled();
        log.debug("CANCELED: {}", conversionItemDTO.getSourceFile());
    }

    private ConvertImageInputDTO buildInput() {
        return ConvertImageInputDTO.builder()
                .sourceFile(conversionItemDTO.getSourceFile())
                .destinationPath(conversionItemDTO.getDestinationDirectory())
                .tmpPath(convertSingleItemTaskInputDTO.tmpPath())
                .targetExtension(conversionItemDTO.getTargetExtension())
                .onStart(this::onStart)
                .onProgress(this::updateProgressFloatValue)
                .fail(this::fail)
                .pass(this::pass)
                .addToFileRenameQueue(convertSingleItemTaskInputDTO.addToFileRenameQueue())
                .addToFileDeleteQueue(convertSingleItemTaskInputDTO.addToFileDeleteQueue())
                .notificationLevel(convertSingleItemTaskInputDTO.notificationLevel())
                .build();
    }

    public void pass() {
        convertSingleItemTaskInputDTO.incrementPasses().run();
        sendSuccessfulDTO();
    }

    public void fail(Exception e) {
        convertSingleItemTaskInputDTO.incrementFailures().run();
        sendFailedDTO(e);
    }

    private void sendFailedDTO(Exception e) {
        conversionItemDTO.setStatus(ConversionStatus.FAILED);
        conversionItemDTO.setProgressPercentage(100f);
        conversionItemDTO.setErrorMessage(getRootCauseMessage(e));
        convertSingleItemTaskInputDTO.setItemAsCompleted().accept(conversionItemDTO);
    }

    private void sendSuccessfulDTO() {
        conversionItemDTO.setStatus(ConversionStatus.COMPLETED);
        conversionItemDTO.setProgressPercentage(100f);
        convertSingleItemTaskInputDTO.setItemAsCompleted().accept(conversionItemDTO);
    }

    private void updateAsCancelled() {
        conversionItemDTO.setStatus(ConversionStatus.FAILED);
        conversionItemDTO.setProgressPercentage(100f);
        conversionItemDTO.setErrorMessage("cancelled");
        convertSingleItemTaskInputDTO.setItemAsCompleted().accept(conversionItemDTO);
    }

    private void updateProgressFloatValue(float progress) {
        if ((progress == 1 || progress % 3 == 0) && Duration.between(lastCall, LocalDateTime.now()).toMillis() >= 1000) {
            conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
            conversionItemDTO.setProgressPercentage(progress);
            lastCall = LocalDateTime.now();
            convertSingleItemTaskInputDTO.publishItemUpdate().accept(conversionItemDTO);
        }
    }

    public void closeStreams() {
        this.canceled = true;
        singleImageConverter.cancel();
    }
}
