package org.andreidodu.nocconvert.task;

import lombok.Getter;
import org.andreidodu.nocconvert.exception.SearchManuallyAbortedException;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public class PictureSearcherTask {
    private static final Logger log = LogManager.getLogger(PictureSearcherTask.class);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private long limiterCounter = 0;
    private final static int LIMITER_INTERVAL = 200;

    public Collection<File> search(Path directory, FilesInDirectoryListener parentListener) throws Exception {

        // NOTE: here we are checking if we have the access to the root directory
        // NOTE: if is negative, then it will throw an exception that will be caught by the caller
        try (var stream = Files.newDirectoryStream(directory)) {
            stream.iterator().next();
        }
        try {
            CustomIOFilter listener = getFileFilter(parentListener);
            Collection<File> result = FileUtils.listFiles(directory.toFile(), listener, TrueFileFilter.TRUE);
            log.debug("Found {} files in directory {}", result.size(), directory);
            if (listener.getError() != null) {
                throw new SearchManuallyAbortedException(listener.getError().getMessage());
            }
            return result;
        } catch (Exception e) {
            throw new SearchManuallyAbortedException(e.getMessage(), e);
        }
    }

    public void cancel() {
        this.cancelled.set(true);
    }

    private CustomIOFilter getFileFilter(FilesInDirectoryListener listener) {
        return new CustomIOFilter(listener);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    private class CustomIOFilter implements IOFileFilter {

        @Getter
        private Exception error;

        private final FilesInDirectoryListener listener;

        public CustomIOFilter(FilesInDirectoryListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean accept(File file) {
            if (cancelled.get()) {
                error = new SearchManuallyAbortedException("manually aborted");
                throw new RuntimeException(error);
            }

            if (file.isDirectory()) {
                return false;
            }

            if (limiterCounter++ % LIMITER_INTERVAL == 0 && !cancelled.get()) {
                listener.onFileFound(file.toPath());
            }

            return true;
        }

        @Override
        public boolean accept(File file, String s) {
            if (cancelled.get()) {
                error = new SearchManuallyAbortedException("manually aborted");
                throw new RuntimeException(error);
            }
            if (limiterCounter++ % LIMITER_INTERVAL == 0 && !cancelled.get()) {
                listener.onFileFound(file.toPath());
            }
            return true;
        }

    }


}
