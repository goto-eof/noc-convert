package org.andreidodu.nocconvert.task;

import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;

import java.io.IOException;

public interface ImageConverterTask {

    void convertImage(ConvertImageInputDTO convertImageInputDTO) throws IOException;

    void interruptTask();
}
