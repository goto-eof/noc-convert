package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FileSystemServiceImpl implements FileSystemService {
    private static final Logger log = LogManager.getLogger(FileSystemServiceImpl.class);

    public List<Path> getAllFilesInDirectory(Path directoryPath,
                                             List<String> allowedFileExtensionList,
                                             FilesInDirectoryListener listener,
                                             Supplier<Boolean> isCancelled
    ) {
        Objects.requireNonNull(directoryPath, "Directory path must not be null");

        if (!Files.isDirectory(directoryPath) || !isContainFiles(directoryPath)) {
            return new ArrayList<>();
        }

        List<Path> result = new ArrayList<>();
        Queue<Path> queueOfDirectories = new LinkedList<>();
        queueOfDirectories.add(directoryPath);

        long totalFiles = 0;
        while (!queueOfDirectories.isEmpty()) {
            Path currentDir = queueOfDirectories.remove();

            long filesInThisDir = 0;
            try (var stream = Files.newDirectoryStream(currentDir)) {
                for (Path entry : stream) {
                    try {
                        if (Files.isDirectory(entry)) {
                            queueOfDirectories.add(entry);
                        } else if (isValidFile(entry, allowedFileExtensionList)) {
                            result.add(entry);
                            listener.onFileFound(entry);
                            filesInThisDir++;
                        }
                        if (isCancelled.get()) {
                            listener.onOperationAborted();
                            return result;
                        }
                    } catch (Exception e) {
                        log.warn("not valid path: {}", entry);
                    }
                }
                totalFiles += filesInThisDir;
                listener.onDirectoryProcessed(totalFiles, currentDir, filesInThisDir);

            } catch (IOException e) {
                log.warn("unaccessible directory: {}", currentDir);
            }
            listener.onUpdateTotalFile(totalFiles);
        }
        listener.onSearchDone(result);
        return result;
    }

    private static boolean isValidFile(Path file, List<String> allowedFileExtensionList) {
        try {
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                return false;
            }

            String name = file.getFileName().toString().toLowerCase();
            return allowedFileExtensionList.stream()
                    .anyMatch(name::endsWith);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
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

    private boolean isContainFiles(Path directoryPath) {
        return directoryPath.toFile().list() != null && directoryPath.toFile().list().length > 0;
    }


}
