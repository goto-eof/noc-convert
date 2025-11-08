package org.andreidodu.nocconvert.helper;

import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestHelper {

    public static List<Path> prepareInputFiles(Path resourcePath, Path tempInputFolder) throws IOException, URISyntaxException {
        List<Path> conversionFileList = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(resourcePath)) {
            files = walk
                    .filter(Files::isRegularFile)
                    .toList();
        }

        files.forEach(file -> {
            String onlyFilename = file.getFileName().toString();
            try {
                Path destinationPath = copyFile(file, onlyFilename, tempInputFolder);
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
        return conversionFileList;
    }

    public static Path getResourcePath(String directoryName) throws IOException, URISyntaxException {
        URL resourceUrl = TestHelper.class.getClassLoader().getResource(directoryName);

        if (resourceUrl == null) {
            throw new IOException("Resource directory not found: " + "test_images");
        }

        return Paths.get(resourceUrl.toURI());
    }

    private static Path copyFile(Path source, String destination, Path tempInputFolder) throws IOException {
        Path destinationPath = tempInputFolder.resolve(destination);
        Files.copy(source, destinationPath);
        return destinationPath;
    }

    private static Path getResourcePath(String resourceName, Path tempInputFolder) throws IOException {
        URL resourceUrl = TestHelper.class.getClassLoader().getResource(resourceName);

        if (resourceUrl == null) {
            throw new IOException("Resource not found: " + resourceName);
        }

        try {
            return Paths.get(resourceUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Error converting URL to URI for resource: " + resourceName, e);
        }
    }

    public static List<String> getAllAvailableWriteFormats() {
        return new ImageConverterUtil().getAvailableWriteFormatList()
                .stream()
                .map(FormatExtensionDTO::getFormat)
                .toList();
    }
}
