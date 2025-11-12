package org.andreidodu.nocconvert.gui.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertSingleItemTaskInputDTO;
import org.andreidodu.nocconvert.exception.ConversionManualAbortedException;
import org.andreidodu.nocconvert.task.SingleImageConverterComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class SingleItemConvertionTask implements Runnable {
    private static final Logger log = LogManager.getLogger(SingleItemConvertionTask.class);
    private final ConversionItemDTO conversionItemDTO;
    private final SingleImageConverterComponent singleImageConverter;
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO;
    LocalDateTime lastCall = LocalDateTime.now();


    public SingleItemConvertionTask(ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO) {
        this.convertSingleItemTaskInputDTO = convertSingleItemTaskInputDTO;
        this.conversionItemDTO = convertSingleItemTaskInputDTO.conversionItemDTO();
        this.singleImageConverter = new SingleImageConverterComponent(convertSingleItemTaskInputDTO.platformExecutorService());
    }


    private void onStart() {
        conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
        conversionItemDTO.setProgressPercentage(0f);
        convertSingleItemTaskInputDTO.publishItemUpdate().accept(conversionItemDTO);
    }

    public void run() {
        if (canceled.get() || Thread.currentThread().isInterrupted() || convertSingleItemTaskInputDTO.isParentInterrupted().get()) {
            log.debug("thread is cancelled");
            convertSingleItemTaskInputDTO.semaphore().release();
            convertSingleItemTaskInputDTO.countFinish().countDown();
            throw new ConversionManualAbortedException("operation aborted");
        }
        try {
            ConvertImageInputDTO convertImageInputDTO = buildInput();
            this.singleImageConverter.convertImage(convertImageInputDTO, conversionItemDTO);
        } catch (ConversionManualAbortedException e) {
            log.error("operation aborted b1: {}", e.getMessage());
            cancel();
            throw new ConversionManualAbortedException("operation aborted");
        } catch (Exception e) {
            cancel();
            log.error("aborted?:{}", e.getMessage());
            throw new ConversionManualAbortedException("operation aborted");
        } finally {
            convertSingleItemTaskInputDTO.semaphore().release();
            convertSingleItemTaskInputDTO.countFinish().countDown();
        }
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
                .renameMe(convertSingleItemTaskInputDTO.renameMe())
                .copyMe(convertSingleItemTaskInputDTO.copyMe())
                .calculateTemporaryFilenameForMe(convertSingleItemTaskInputDTO.calculateTemporaryFilenameForMe())
                .notificationLevel(convertSingleItemTaskInputDTO.notificationLevel())
                .readerFormatNames(convertSingleItemTaskInputDTO.readerFormatNames())
                .addToFileQueue(convertSingleItemTaskInputDTO.processedFilesQueue())
                .build();
    }

    public void pass(ConversionItemDTO conversionItemDTO) {
        convertSingleItemTaskInputDTO.incrementPasses().run();
        conversionItemDTO.setStatus(ConversionStatus.COMPLETED);
        conversionItemDTO.setProgressPercentage(100f);
        convertSingleItemTaskInputDTO.setItemAsCompleted().accept(conversionItemDTO);
        convertSingleItemTaskInputDTO.processedFilesQueue().accept(conversionItemDTO);

    }

    public void fail(ConversionItemDTO conversionItemDTO, Exception e) {
        convertSingleItemTaskInputDTO.incrementFailures().run();
        sendFailedDTO(conversionItemDTO, e);
        convertSingleItemTaskInputDTO.processedFilesQueue().accept(conversionItemDTO);
    }

    private void sendFailedDTO(ConversionItemDTO conversionItemDTO, Exception e) {
        conversionItemDTO.setStatus(ConversionStatus.FAILED);
        conversionItemDTO.setProgressPercentage(100f);
        conversionItemDTO.setErrorMessage(getRootCauseMessage(e));
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

    public void cancel() {
        this.canceled.set(true);
        singleImageConverter.cancel();
    }
}
