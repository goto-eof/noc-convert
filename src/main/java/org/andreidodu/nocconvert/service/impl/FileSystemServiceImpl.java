package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.exception.SearchManuallyAbortedException;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.service.FileSystemService;
import org.andreidodu.nocconvert.util.ThreadUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FileSystemServiceImpl implements FileSystemService {
    private static final Logger log = LogManager.getLogger(FileSystemServiceImpl.class);
    public static final int YIELD_INTERVAL = 5;

    public List<Path> getAllFilesInDirectory(Path directoryPath,
                                             List<String> allowedFileExtensionList,
                                             FilesInDirectoryListener listener,
                                             Supplier<Boolean> isCancelled
    ) throws IOException {
        Objects.requireNonNull(directoryPath, "Directory path must not be null");

        // check if access allowed or denied
        try (var stream = Files.newDirectoryStream(directoryPath)) {
            stream.iterator().next();
        }

        if (!Files.isDirectory(directoryPath) || !isContainFiles(directoryPath)) {
            return new ArrayList<>();
        }

        List<Path> result = new ArrayList<>();
        Queue<Path> queueOfDirectories = new LinkedList<>();
        queueOfDirectories.add(directoryPath);

        long yieldCounter = 0;

        long totalFiles = 0;
        while (!queueOfDirectories.isEmpty()) {
            Path currentDir = queueOfDirectories.remove();

            long filesInThisDir = 0;
            try (var stream = Files.newDirectoryStream(currentDir)) {
                for (Path entry : stream) {
                    try {

                        if (Files.isDirectory(entry) && !entry.getFileName().startsWith(".")) {
                            queueOfDirectories.add(entry);
                        } else if (isValidFile(entry, allowedFileExtensionList)) {
                            result.add(entry);
                            listener.onFileFound(entry);
                            filesInThisDir++;
                        }

                        if (yieldCounter % YIELD_INTERVAL == 0) {
                            yieldCooperatively(listener, isCancelled);
                            yieldCounter++;
                        }

                    } catch (Exception e) {
                        if (e instanceof SearchManuallyAbortedException) {
                            throw e;
                        }

                        log.warn("not valid path: {}", entry);
                    }

                }

                totalFiles += filesInThisDir;
                listener.onDirectoryProcessed(totalFiles, currentDir, filesInThisDir);

            } catch (AccessDeniedException ee) {
                log.warn("unaccessible directory: {} because {}", currentDir, ee.getMessage(), ee);
            } catch (IOException e) {
                log.error("IOException directory: {} because {}", currentDir, e.getMessage(), e);
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
            }
            listener.onUpdateTotalFile(totalFiles);
        }
        listener.onSearchDone(result);
        return result;
    }

    private static void yieldCooperatively(FilesInDirectoryListener listener, Supplier<Boolean> isCancelled) {
        ThreadUtil.yieldCooperatively();
        if (isCancelled.get() || Thread.currentThread().isInterrupted()) {
            listener.onOperationAborted();
            throw new SearchManuallyAbortedException("search process manually aborted");
        }
    }

    private static boolean isValidFile(Path file, List<String> allowedFileExtensionList) {
        try {
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                return false;
            }

            String name = file.getFileName().toString().toLowerCase();
            return allowedFileExtensionList.stream()
                    .anyMatch(ext -> name.endsWith("." + ext.toLowerCase()));
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
        return directoryPath.toFile().list() != null && Objects.requireNonNull(directoryPath.toFile().list()).length > 0;
    }


}
