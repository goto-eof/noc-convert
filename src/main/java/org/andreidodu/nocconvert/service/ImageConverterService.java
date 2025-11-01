package org.andreidodu.nocconvert.service;

import org.andreidodu.nocconvert.dto.conversion.input.ConvertImageInputDTO;

import java.io.IOException;

public interface ImageConverterService {

    void convertImage(ConvertImageInputDTO convertImageInputDTO) throws IOException;

    void interruptTask();
}
