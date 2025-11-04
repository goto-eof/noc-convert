package org.andreidodu.nocconvert.task;

import org.andreidodu.nocconvert.exception.SearchManuallyAbortedException;
import org.andreidodu.nocconvert.listener.FilesInDirectoryListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PictureSearcher {

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public Collection<File> search(Path directory, FilesInDirectoryListener listener, List<String> allowedExtensions) {
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
