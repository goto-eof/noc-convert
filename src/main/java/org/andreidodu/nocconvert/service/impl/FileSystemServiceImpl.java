package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.service.FileSystemService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class FileSystemServiceImpl implements FileSystemService {
    private static final Logger log = LogManager.getLogger(FileSystemServiceImpl.class);

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
