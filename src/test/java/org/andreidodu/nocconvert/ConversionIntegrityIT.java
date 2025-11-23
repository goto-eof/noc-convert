package org.andreidodu.nocconvert;

import org.andreidodu.nocconvert.dto.ConversionItemDTO;
import org.andreidodu.nocconvert.dto.IntWrapper;
import org.andreidodu.nocconvert.gui.GUIOrchestrator;
import org.andreidodu.nocconvert.gui.components.SplitButtonComponent;
import org.andreidodu.nocconvert.gui.controller.ConversionController;
import org.andreidodu.nocconvert.gui.dto.ConversionDTO;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.mapper.ConcurrentItemDTOMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.andreidodu.nocconvert.constants.ApplicationConfig.FINAL_OUTPUT_DIRECTORY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


public class ConversionIntegrityIT {

    private static final Log log = LogFactory.getLog(ConversionIntegrityIT.class);
    @TempDir
    Path tempInputFolder;

    @TempDir
    Path tempOutputFolder;

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
    private final int VALID_FILES_TO_CREATE = 10;


    private ConversionController controller;
    private List<ConversionItemDTO> conversionItemDTOList;

    @BeforeEach
    void setUp() throws IOException, URISyntaxException {
        MockitoAnnotations.openMocks(this);


        List<Path> conversionFileList = new ArrayList<>();
        URL resourceUrl = getClass().getClassLoader().getResource("images");

        if (resourceUrl == null) {
            throw new IOException("Resource directory not found: " + "images");
        }

        Path resourcePath = Paths.get(resourceUrl.toURI());
        List<Path> files;
        try (Stream<Path> walk = Files.walk(resourcePath)) {
            files = walk
                    .filter(Files::isRegularFile)
                    .toList();
        }

        files.forEach(file -> {
            String onlyFilename = file.getFileName().toString();
            try {
                Path destinationPath = copyFile(file, onlyFilename);
                conversionFileList.add(destinationPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

        Path tmpInvalidFile = tempInputFolder.resolve("not-a-picture-1.ico");
        Files.createFile(tmpInvalidFile);
        conversionFileList.add(tmpInvalidFile);

        tmpInvalidFile = tempInputFolder.resolve("not-a-picture-2.gif");
        Files.createFile(tmpInvalidFile);
        conversionFileList.add(tmpInvalidFile);

        tmpInvalidFile = tempInputFolder.resolve("not-a-picture-2.bmp");
        Files.createFile(tmpInvalidFile);
        conversionFileList.add(tmpInvalidFile);

        conversionItemDTOList = ConcurrentItemDTOMapper.convertPathListToDTOList(tempOutputFolder, "png", conversionFileList, new IntWrapper(0));
        when(mockedSplitButtonComponent.getDropdownToggleButton()).thenReturn(mockedButton);
        when(mockedSplitButtonComponent.getMainActionButton()).thenReturn(mockedButton);

        ConversionDTO conversionDTO = ConversionDTO.builder()
                .guiOrchestrator(mockedGuiOrchestrator)
                .convertComponent(mockedSplitButtonComponent)
                .applicationStatusLabel(mockedStatusLabel)
                .secondaryApplicationStatusLabel(mockedSecondaryApplicationStatusLabel)
                .conversionFileJList(mockedConversionFileJList)
                .build();
        when(mockedGuiOrchestrator.getSourceDirectory()).thenReturn(tempInputFolder);
        when(mockedGuiOrchestrator.getDestinationDirectory()).thenReturn(tempOutputFolder);
        when(conversionDTO.convertComponent().getSelectedItem()).thenReturn(new FormatExtensionDTO("ico", "ico"));
        this.controller = new ConversionController(conversionDTO);
    }

    private Path copyFile(Path source, String destination) throws IOException {
        Path destinationPath = tempInputFolder.resolve(destination);
        // Path resource = getResourcePath(source);
        Files.copy(source, destinationPath);
        return destinationPath;
    }

    private Path getResourcePath(String resourceName) throws IOException {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);

        if (resourceUrl == null) {
            throw new IOException("Resource not found: " + resourceName);
        }

        try {
            return Paths.get(resourceUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Error converting URL to URI for resource: " + resourceName, e);
        }
    }

    @Test
    void shouldMatchInternalAndFileSystemSuccessCounts() throws InterruptedException, ExecutionException, IOException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        controller.startConversion(countDownLatch);
        countDownLatch.await();

        long actualFilesOnFS;
        Path tmpDirectory = Path.of(tempOutputFolder.toString(), FINAL_OUTPUT_DIRECTORY_NAME);

        List<Path> filess = Files.list(tmpDirectory).toList();
        log.debug("files: " + filess);
        log.debug("tmpDirectory: " + mockedGuiOrchestrator.getDestinationDirectory());

        try (Stream<Path> files = Files.list(tmpDirectory)) {
            actualFilesOnFS = files.filter(Files::isRegularFile).count();
        }

        int successFilesFromLI = controller.getInternalSuccessCount();

        assertEquals(successFilesFromLI, actualFilesOnFS, "the number of successfully processed images on FS and GUI are not aligned The success outcome does not match");
        assertEquals(VALID_FILES_TO_CREATE, successFilesFromLI, "the number of successfully processed images is not expected");
    }
}