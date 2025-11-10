package org.andreidodu.nocconvert;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.controller.ConversionController;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.helper.TestHelper;
import org.andreidodu.nocconvert.mapper.ConcurrentItemDTOMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.andreidodu.nocconvert.helper.TestHelper.prepareInputFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


public class OutputFormatsTestIT {

    private static final long NUMBER_FILES_IN_TEST_DIRECTORY = 10;

    @TempDir
    Path temporaryInputFolder;

    @TempDir
    Path temporaryOutputFolder;

    @Mock
    private SplitButtonComponent mockedSplitButtonComponent;

    @Mock
    private JLabel mockedStatusLabel;

    @Mock
    private JLabel mockedSecondaryApplicationStatusLabel;

    @Mock
    private JList<ConversionItemDTO> mockedConversionFileJList;

    @Mock
    private GUIOrchestrator mockedGuiOrchestrator;

    @Mock
    private JButton mockedButton;

    @Mock
    private GUIOrchestrator guiOrchestrator;

    private List<Path> sourceFileList;

    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        MockitoAnnotations.openMocks(this);
        sourceFileList = prepareInputFiles(TestHelper.getResourcePath("images"), temporaryInputFolder);
        when(mockedSplitButtonComponent.getDropdownToggleButton()).thenReturn(mockedButton);
        when(mockedSplitButtonComponent.getMainActionButton()).thenReturn(mockedButton);

        doNothing().when(mockedButton).setEnabled(Mockito.anyBoolean());
        doNothing().when(mockedGuiOrchestrator).setEnableSearchStepComponents(Mockito.anyBoolean());
    }

    @ParameterizedTest
    @MethodSource("org.andreidodu.nocconvert.helper.TestHelper#getAllAvailableWriteFormats")
    void shouldTestAllImageFormats(String targetFileFormat) throws ExecutionException, InterruptedException, IOException {

        ConversionController conversionController = prepareConversionController();

        List<ConversionItemDTO> conversionItemDTOList = ConcurrentItemDTOMapper.convertPathListToDTOList(temporaryOutputFolder, targetFileFormat, sourceFileList);

        conversionController.startConversion(conversionItemDTOList);
        conversionController.getWorker().get();

        long successFilesFomFileSystem = countFilesInTmpDirectory(temporaryOutputFolder);
        long successFilesFromController = conversionController.getInternalSuccessCount();

        assertEquals(successFilesFromController, successFilesFomFileSystem, "The number of File System files does not match with the controller's one");
        assertEquals(NUMBER_FILES_IN_TEST_DIRECTORY, successFilesFomFileSystem, "The number of File System files does not match with the one expected by developer");
    }

    public long countFilesInTmpDirectory(Path temporaryOutputFolder) throws IOException {
        try (Stream<Path> files = Files.list(temporaryOutputFolder)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private ConversionController prepareConversionController() {
        ConversionDTO conversionDTO = ConversionDTO.builder()
                .guiOrchestrator(guiOrchestrator)
                .convertComponent(mockedSplitButtonComponent)
                .applicationStatusLabel(mockedStatusLabel)
                .secondaryApplicationStatusLabel(mockedSecondaryApplicationStatusLabel)
                .conversionFileJList(mockedConversionFileJList)
                .build();

        return new ConversionController(conversionDTO);
    }
}