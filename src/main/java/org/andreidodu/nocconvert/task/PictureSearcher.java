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
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PictureSearcher {
    private static final Logger log = LogManager.getLogger(PictureSearcher.class);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public Collection<File> search(Path directory, FilesInDirectoryListener listener, List<String> allowedExtensions) throws IOException {

        try (var stream = Files.newDirectoryStream(directory)) {
            stream.iterator().next();
        }

        return FileUtils.listFiles(directory.toFile(), getFileFilter(listener, allowedExtensions), TrueFileFilter.TRUE);
    }

    public void cancel() {
        this.cancelled.set(true);
    }

    private IOFileFilter getFileFilter(FilesInDirectoryListener listener, List<String> allowedExtensions) {
        return new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                if (cancelled.get()) {
                    throw new SearchManuallyAbortedException("operation aborted");
                }
                if (file.isDirectory()) {
                    return false;
                }
                String name = file.getName().toLowerCase();
                return allowedExtensions.stream()
                        .anyMatch(ext -> name.toLowerCase().endsWith("." + ext.toLowerCase()));
            }

            @Override
            public boolean accept(File file, String s) {
                if (cancelled.get()) {
                    throw new SearchManuallyAbortedException("operation aborted");
                }
                boolean accept = allowedExtensions.stream()
                        .anyMatch(ext -> s.toLowerCase().endsWith("." + ext.toLowerCase()));
                listener.onFileFound(file.toPath());
                return accept;
            }

        };
    }


}
