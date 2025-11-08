package org.andreidodu.nocconvert.task;

import org.andreidodu.nocconvert.exception.SearchManuallyAbortedException;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public class PictureSearcher {
    private static final Logger log = LogManager.getLogger(PictureSearcher.class);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private long limiterCounter = 0;
    private final static int LIMITER_INTERVAL = 200;

    public Collection<File> search(Path directory, FilesInDirectoryListener listener) throws IOException {

        // NOTE: here we are checking if we have the access to the root directory
        // NOTE: if is negative, then it will throw an exception that will be caught by the caller
        try (var stream = Files.newDirectoryStream(directory)) {
            stream.iterator().next();
        }

        return FileUtils.listFiles(directory.toFile(), getFileFilter(listener), TrueFileFilter.TRUE);
    }

    public void cancel() {
        this.cancelled.set(true);
    }

    private IOFileFilter getFileFilter(FilesInDirectoryListener listener) {
        return new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                if (cancelled.get()) {
                    throw new SearchManuallyAbortedException("manually aborted");
                }

                if (file.isDirectory()) {
                    return false;
                }

                if (limiterCounter++ % LIMITER_INTERVAL == 0) {
                    listener.onFileFound(file.toPath());
                }

                return true;
            }

            @Override
            public boolean accept(File file, String s) {
                if (cancelled.get()) {
                    throw new SearchManuallyAbortedException("manually aborted");
                }
                if (limiterCounter++ % LIMITER_INTERVAL == 0) {
                    listener.onFileFound(file.toPath());
                }
                return true;
            }

        };
    }


}
