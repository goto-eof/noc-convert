package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.service.FileSystemService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSystemServiceImpl implements FileSystemService {
    private static final Logger log = LogManager.getLogger(FileSystemServiceImpl.class);

    @Override
    public List<Path> getAllFiles(Path directoryPath, List<String> allowedFileExtensionList, BiConsumer<Path, Integer> onDirectoryProcessed) {
        Objects.requireNonNull(directoryPath, "Directory path must not be null");
        Objects.requireNonNull(allowedFileExtensionList, "Allowed file extension list must not be null");
        Objects.requireNonNull(onDirectoryProcessed, "On directory processed must not be null");

        try (Stream<Path> pathStream = Files.walk(directoryPath)) {

            Map<Path, List<Path>> parentAndFilesMap = pathStream
                    .parallel()
                    .filter(file -> isValidFile(file, allowedFileExtensionList))
                    .collect(Collectors.groupingBy(Path::getParent));

            List<Path> result = new ArrayList<>();

            for (Map.Entry<Path, List<Path>> entry : parentAndFilesMap.entrySet()) {
                result.addAll(entry.getValue());
                onDirectoryProcessed.accept(entry.getKey(), entry.getValue().size());
            }

            return result;
        } catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private static boolean isValidFile(Path file, List<String> allowedFileExtensionList) {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        String name = file.getFileName().toString().toLowerCase();
        return allowedFileExtensionList.stream()
                .anyMatch(name::endsWith);
    }

    @Override
    public boolean containsAtLeaseOneFile(Path directoryPath) {
        Objects.requireNonNull(directoryPath, "Directory path must not be null");

        try (Stream<Path> files = Files.list(directoryPath)) {
            return files.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
