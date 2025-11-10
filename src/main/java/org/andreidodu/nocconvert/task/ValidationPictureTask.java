package org.andreidodu.nocconvert.task;

import org.andreidodu.nocconvert.exception.ManualAbortedException;
import org.andreidodu.nocconvert.gui.worker.ImageSearcherSwingWorker;
import org.andreidodu.nocconvert.helper.ImageUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class ValidationPictureTask {
    private static final Logger log = LogManager.getLogger(ValidationPictureTask.class);
    public static final int MAX_CONCURRENT_THREADS = 15;
    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final AtomicLong progressCounter = new AtomicLong();
    private final static int LIMITER_INTERVAL = 200;
    public final Supplier<Boolean> isCancelled;
    private final List<Path> inputList;
    private final ImageSearcherSwingWorker.FilesInDirectoryListenerImpl filesInDirectoryListener;

    public ValidationPictureTask(List<Path> inputList, ImageSearcherSwingWorker.FilesInDirectoryListenerImpl filesInDirectoryListener, Supplier<Boolean> isCancelled) {
        this.inputList = inputList;
        this.filesInDirectoryListener = filesInDirectoryListener;
        this.isCancelled = isCancelled;
    }

    public List<Path> validateAndGetSearchResult() {
        try {
            if (canceled.get()) {
                throw new ManualAbortedException("operation aborted");
            }

            return getValidImages();
        } catch (Exception e) {
            throw new ManualAbortedException(getRootCauseMessage(e), e);
        }
    }

    private List<Path> getValidImages() throws InterruptedException {
        if (this.inputList.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<Path> imagesList = new ArrayList<>(this.inputList);

        ConcurrentLinkedQueue<Path> validImages = new ConcurrentLinkedQueue<>();
        filesInDirectoryListener.onFileAnalyzationStart(this.inputList.size());

        log.debug("start submission...");
        int numOfRounds = this.inputList.size() / MAX_CONCURRENT_THREADS;
        log.debug("start rounds submission {}", numOfRounds);
        int rounds = this.inputList.size() % MAX_CONCURRENT_THREADS == 0 ? numOfRounds : numOfRounds + 1;
        for (int i = 0; i < rounds; i++) {
            log.debug("round {}", i);

            try {
                ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
                int startIndex = i * MAX_CONCURRENT_THREADS;
                int endIndex = Math.min((i + 1) * MAX_CONCURRENT_THREADS, this.inputList.size());
                Collection<Path> sublist = imagesList.subList(startIndex, endIndex);
                for (Path path : sublist) {
                    if (canceled.get()) {
                        executorService.shutdownNow();
                        throw new ManualAbortedException("operation aborted");
                    }
                    executorService.submit(() -> validationTask(path, validImages));
                }
                executorService.shutdown();
                boolean res = executorService.awaitTermination(31, TimeUnit.DAYS);
                if (!res) {
                    executorService.shutdownNow();
                }
            } catch (Exception e) {
                throw e;
            }
        }
        log.debug("end submission...");
        return validImages.stream().toList();
    }


    private boolean validationTask(Path path, ConcurrentLinkedQueue<Path> outcomeList) {
        if (canceled.get()) {
            return false;
        }

        if (path.toFile().length() == 0) {
            progressCounter.incrementAndGet();
            filesInDirectoryListener.onFileAnalyzationComplete(path);
            return true;
        }
        try {
            return analyzeFileHeaderAndAddToQueue(path, outcomeList);
        } catch (ManualAbortedException e) {
            log.error("Error while processing image file header", e);
            return false;
        }
    }

    private boolean analyzeFileHeaderAndAddToQueue(Path path, ConcurrentLinkedQueue<Path> validImages) {
        if (canceled.get() || Thread.currentThread().isInterrupted() || isCancelled.get()) {
            throw new ManualAbortedException("operation aborted");
        }
        try {
            if (hasValidImageHeaders(path)) {
                validImages.add(path);
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            filesInDirectoryListener.onFileAnalyzationComplete(path);
        }
        //filesInDirectoryListener.onFileAnalyzationProgress();
    }


    private static boolean hasValidImageHeaders(Path file) {
        try {
            return ImageUtil.getImageHeaders(file).isHeaderFound();
        } catch (Exception e) {
            return false;
        }
    }

    public void cancel() {
        this.canceled.set(true);
    }

}
