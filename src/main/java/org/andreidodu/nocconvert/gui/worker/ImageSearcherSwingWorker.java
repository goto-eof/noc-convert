package org.andreidodu.nocconvert.gui.worker;

import lombok.Builder;
import org.andreidodu.nocconvert.exception.ManualAbortedException;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.andreidodu.nocconvert.task.PictureSearcher;
import org.andreidodu.nocconvert.helper.ImageUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ImageSearcherSwingWorker extends SwingWorker<Collection<File>, ImageSearcherSwingWorker.ChunkDTO> {
    private static final Logger log = LogManager.getLogger(ImageSearcherSwingWorker.class);
    public static final int MAC_CONCURRENT_THREADS = 50;

    private final Path sourceDirectory;
    private final FilesInDirectoryListener filesInDirectoryListener;
    private final PictureSearcher pictureSearcher;
    private final AtomicLong progressCounter = new AtomicLong();
    private final static int LIMITER_INTERVAL = 200;
    private final AtomicBoolean canceled = new AtomicBoolean(false);

    private STATUS status = STATUS.SEARCHING;

    enum STATUS {
        SEARCHING, ANALYZING
    }

    public ImageSearcherSwingWorker(Path sourceDirectory, FilesInDirectoryListener filesInDirectoryListener) {
        super();
        this.sourceDirectory = sourceDirectory;
        this.filesInDirectoryListener = filesInDirectoryListener;
        this.pictureSearcher = new PictureSearcher();
    }

    @Override
    protected Collection<File> doInBackground() {
        log.debug("Starting to search image from directory {}", sourceDirectory);
        FilesInDirectoryListenerImpl listener = new FilesInDirectoryListenerImpl();
        try {
            listener.onSearchDone(pictureSearcher.search(sourceDirectory, listener).stream().map(File::toPath).toList());
            log.debug("Finished searching image from directory {}", sourceDirectory);
        } catch (AccessDeniedException e) {
            listener.onAccessDenied();
            log.error("Access denied for: {}", sourceDirectory, e);
        } catch (Exception e) {
            log.error(getRootCauseMessage(e), e);
            listener.onUpdateTotalFile(0L);
            listener.onSearchDone(new ArrayList<>());
        }
        return new ArrayList<>();
    }

    @Override
    protected void done() {
        // NOTE: we're already passing the result through the FilesInDirectoryListenerImpl
    }

    public Void shutdown() {
        pictureSearcher.cancel();
        this.canceled.set(true);
        this.cancel(true);
        Thread.currentThread().interrupt();
        return null;
    }

    @Builder
    protected record ChunkDTO(Path directory, long numFiles) {

    }

    @Override
    protected void process(List<ChunkDTO> chunks) {
        Path directory = chunks.getLast().directory;
        if (status == STATUS.SEARCHING) {
            filesInDirectoryListener.onFileFound(directory);
        } else if (status == STATUS.ANALYZING) {
            filesInDirectoryListener.onFileAnalyzation(directory);
        }
    }

    private class FilesInDirectoryListenerImpl implements FilesInDirectoryListener {
        @Override
        public void onDirectoryProcessed(long totalFiles, Path directory, long filesInDirectory) {
            publish(ChunkDTO.builder().directory(directory).numFiles(filesInDirectory).build());
            filesInDirectoryListener.onDirectoryProcessed(totalFiles, directory, filesInDirectory);
        }

        @Override
        public void onFileFound(Path filename) {
            publish(ChunkDTO.builder().directory(filename).build());
        }

        @Override
        public void onFileAnalyzationStart(int totalNumberFiles) {
            status = STATUS.ANALYZING;
            filesInDirectoryListener.onFileAnalyzationStart(totalNumberFiles);
        }

        @Override
        public void onFileAnalyzationProgress() {
            filesInDirectoryListener.onFileAnalyzationProgress();
        }

        @Override
        public void onFileAnalyzation(Path filename) {
            //  filesInDirectoryListener.onFileAnalyzation(filename);
            publish(ChunkDTO.builder().directory(filename).build());
        }

        @Override
        public void onUpdateTotalFile(Long numberOfDirectories) {
            filesInDirectoryListener.onUpdateTotalFile(numberOfDirectories);
        }

        @Override
        public void onSearchDone(List<Path> result) {
            List<Path> validatedImages = getValidImages(result);
            if (canceled.get()) {
                filesInDirectoryListener.onOperationAborted();
                return;
            }
            filesInDirectoryListener.onSearchDone(validatedImages);
        }

        @Override
        public void onOperationAborted() {
            filesInDirectoryListener.onOperationAborted();
        }

        @Override
        public void onAccessDenied() {
            filesInDirectoryListener.onAccessDenied();
        }
    }

    private List<Path> getValidImages(Collection<Path> images) {
        if (images.isEmpty()) {
            return Collections.emptyList();
        }
        ConcurrentLinkedQueue<Path> validImages = new ConcurrentLinkedQueue<>();
        filesInDirectoryListener.onFileAnalyzationStart(images.size());
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

        try {
            Semaphore semaphore = new Semaphore(MAC_CONCURRENT_THREADS);
            Iterator<Path> iterator = images.iterator();

            while (!canceled.get() && iterator.hasNext() && !Thread.currentThread().isInterrupted() && !isCancelled()) {
                Path path = iterator.next();
                // for (Path path : images) {
                log.debug("Semaphore length: {}", semaphore.availablePermits());

                if (canceled.get()) {
                    shutdownTemporaryExecutor(executorService);
                    Thread.currentThread().interrupt();
                    throw new InterruptedException();
                }

                if (!canceled.get()) {
                    boolean isAcquired = acquireSemaphore(semaphore);
                    if (!isAcquired) {
                        shutdownTemporaryExecutor(executorService);
                        Thread.currentThread().interrupt();
                        throw new InterruptedException();
                    }
                }

                if (canceled.get()) {
                    semaphore.release();
                    shutdownTemporaryExecutor(executorService);
                    Thread.currentThread().interrupt();
                    throw new InterruptedException();
                }

                if (!canceled.get() && !Thread.currentThread().isInterrupted()) {

                    executorService.submit(() -> {
                        log.debug("Semaphore length (VT start): {}", semaphore.availablePermits());
                        if (canceled.get()) {
                            semaphore.release();
                            Thread.currentThread().interrupt();
                        }

                        if (path.toFile().length() == 0) {
                            progressCounter.incrementAndGet();
                            filesInDirectoryListener.onFileAnalyzationProgress();
                            semaphore.release();
                            Thread.currentThread().interrupt();
                        }
                        try {
                            analyzeFileHeaderAndAddToQueue(path, semaphore, validImages);
                        } catch (Exception e) {
                            log.error("Error while processing image file header", e);
                        } finally {
                            semaphore.release();
                        }
                    });
                } else {
                    shutdownTemporaryExecutor(executorService);
                }
            }
        } catch (InterruptedException e) {
            log.error(getRootCauseMessage(e), e);
        }

        executorService.shutdown();
        try {
            boolean res = executorService.awaitTermination(31, TimeUnit.DAYS);
            if (!res) {
                executorService.shutdownNow();

            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return validImages.stream().toList();
    }

    private void shutdownTemporaryExecutor(ExecutorService executorService) {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }


    private void analyzeFileHeaderAndAddToQueue(Path path, Semaphore semaphore, ConcurrentLinkedQueue<Path> validImages) throws InterruptedException {
        if (canceled.get() || Thread.currentThread().isInterrupted() || isCancelled()) {
            return;
        }

        log.debug("Semaphore length (VT processing): {}", semaphore.availablePermits());
        if (progressCounter.getAndIncrement() % LIMITER_INTERVAL == 0) {
            filesInDirectoryListener.onFileAnalyzation(path);
        }
        if (hasValidImageHeaders(path)) {
            validImages.add(path);
        }
        filesInDirectoryListener.onFileAnalyzationProgress();
        log.debug("Semaphore length (VT END processing): {}", semaphore.availablePermits());
    }

    private static boolean acquireSemaphore(Semaphore semaphore) throws InterruptedException {
        semaphore.acquire();
        return true;
    }

    private static boolean hasValidImageHeaders(Path file) throws InterruptedException {
        try {
            return ImageUtil.getImageHeaders(file).isHeaderFound();
        } catch (ManualAbortedException e) {
            throw new InterruptedException("Operation aborted");
        } catch (Exception e) {
            return false;
        }
    }

}
