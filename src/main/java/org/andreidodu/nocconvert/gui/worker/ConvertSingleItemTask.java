package org.andreidodu.nocconvert.gui.worker;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.ConversionStatus;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;
import org.andreidodu.nocconvert.dto.conversion.input.ConvertSingleItemTaskInputDTO;
import org.andreidodu.nocconvert.mapper.ConversionItemDTOMapper;
import org.andreidodu.nocconvert.service.ImageConverterService;
import org.andreidodu.nocconvert.service.impl.ImageConverterServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ConvertSingleItemTask implements Runnable {
    private static final Logger log = LogManager.getLogger(ConvertSingleItemTask.class);
    private final ConversionItemDTO conversionItemDTO;
    private final ImageConverterService imageConverterService;
    private boolean canceled = false;
    private final ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO;

    public ConvertSingleItemTask(ConvertSingleItemTaskInputDTO convertSingleItemTaskInputDTO) {
        this.convertSingleItemTaskInputDTO = convertSingleItemTaskInputDTO;
        this.conversionItemDTO = new ConversionItemDTOMapper().clone(convertSingleItemTaskInputDTO.conversionItemDTO());
        this.imageConverterService = new ImageConverterServiceImpl(convertSingleItemTaskInputDTO.platformExecutorService());
    }

    private void onStart() {
        if (!conversionItemDTO.isReadOnly()) {
            conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
            conversionItemDTO.setProgressPercentage(0f);
            convertSingleItemTaskInputDTO.publishItemUpdate().accept(conversionItemDTO);
        }
    }

    public void run() {
        if (canceled) {
            updateAsCancelled();
            return;
        }
        try {
            convertSingleItemTaskInputDTO.semaphore().acquire();
        } catch (InterruptedException e) {
            updateAsCancelled();
            return;
        }
        try {
            ConvertImageInputDTO convertImageInputDTO = buildInput();
            this.imageConverterService.convertImage(convertImageInputDTO);
            if (!conversionItemDTO.isReadOnly()) {
                sendSuccessfullDTO();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if (!conversionItemDTO.isReadOnly()) {
                sendFailedDTO(e);
            }
        } finally {
            convertSingleItemTaskInputDTO.semaphore().release();
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
                .incrementFailures(convertSingleItemTaskInputDTO.incrementFailures())
                .incrementPasses(convertSingleItemTaskInputDTO.incrementPasses())
                .build();
    }


    private void sendFailedDTO(Exception e) {
        conversionItemDTO.setStatus(ConversionStatus.FAILED);
        conversionItemDTO.setProgressPercentage(100f);
        conversionItemDTO.setErrorMessage(getRootCauseMessage(e));
        conversionItemDTO.setReadOnly(true);
        convertSingleItemTaskInputDTO.setItemAsCompleted().accept(conversionItemDTO);
    }

    private void sendSuccessfullDTO() {
        conversionItemDTO.setStatus(ConversionStatus.COMPLETED);
        conversionItemDTO.setProgressPercentage(100f);
        conversionItemDTO.setReadOnly(true);
        convertSingleItemTaskInputDTO.setItemAsCompleted().accept(conversionItemDTO);
    }

    private void updateAsCancelled() {
        if (!conversionItemDTO.isReadOnly()) {
            conversionItemDTO.setStatus(ConversionStatus.FAILED);
            conversionItemDTO.setProgressPercentage(100f);
            conversionItemDTO.setErrorMessage("cancelled");
            convertSingleItemTaskInputDTO.setItemAsCompleted().accept(conversionItemDTO);
        }
    }

    private void updateProgressFloatValue(float progress) {
        // if ((progress == 1 || progress % 3 == 0) && Duration.between(lastCall, LocalDateTime.now()).toMillis() >= 1000) {
        if (!conversionItemDTO.isReadOnly()) {
            conversionItemDTO.setStatus(ConversionStatus.PROCESSING);
            conversionItemDTO.setProgressPercentage(progress);
//        lastCall = LocalDateTime.now();
            convertSingleItemTaskInputDTO.publishItemUpdate().accept(conversionItemDTO);
        }
        //}
    }

    public void closeStreams() {
        this.canceled = true;
        imageConverterService.interruptTask();
    }
}
