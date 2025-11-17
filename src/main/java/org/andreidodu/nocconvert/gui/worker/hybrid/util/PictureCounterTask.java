package org.andreidodu.nocconvert.gui.worker.hybrid.util;

import lombok.Getter;
import org.andreidodu.nocconvert.exception.SearchManuallyAbortedException;
import org.andreidodu.nocconvert.gui.dto.FormatExtensionDTO;
import org.andreidodu.nocconvert.helper.FileUtil;
import org.andreidodu.nocconvert.helper.ImageConverterUtil;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class PictureCounterTask {
    private static final Logger log = LogManager.getLogger(PictureCounterTask.class);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private long limiterCounter = 0;
    private final static int LIMITER_INTERVAL = 200;
    private long count = 0;

    public long count(Path directory, FilesInDirectoryListener parentListener) throws Exception {

        // NOTE: here we are checking if we have the access to the root directory
        // NOTE: if is negative, then it will throw an exception that will be caught by the caller
        try (var stream = Files.newDirectoryStream(directory)) {
            stream.iterator().next();
        }
        try {
            List<String> availableExtensionList = Stream.concat(
                            new ImageConverterUtil().getAvailableReadFormatList()
                                    .stream()
                                    .map(FormatExtensionDTO::getExtension)
                                    .map(String::toLowerCase),
                            Stream.of("jpeg")
                    )
                    .toList();
            CustomIOFilter listener = getFileFilter(parentListener, availableExtensionList);
            Collection<File> result = FileUtils.listFiles(directory.toFile(), listener, TrueFileFilter.TRUE);
            log.debug("Found {} files in directory {}", result.size(), directory);
            if (listener.getError() != null) {
                throw new SearchManuallyAbortedException(listener.getError().getMessage());
            }
            return count;
        } catch (Exception e) {
            throw new SearchManuallyAbortedException(e.getMessage(), e);
        }
    }

    public void cancel() {
        this.cancelled.set(true);
    }

    private CustomIOFilter getFileFilter(FilesInDirectoryListener listener, List<String> availableExtensionList) {
        return new CustomIOFilter(listener, availableExtensionList);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    private class CustomIOFilter implements IOFileFilter {

        @Getter
        private Exception error;

        private final FilesInDirectoryListener listener;
        private final List<String> availableExtensionList;

        public CustomIOFilter(FilesInDirectoryListener listener, List<String> availableExtensionList) {
            this.listener = listener;
            this.availableExtensionList = availableExtensionList;
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

            boolean isOk = availableExtensionList.contains(FileUtil.getExtension(file.getName()));
            if (isOk) {
                count++;
            }

            if (isOk && limiterCounter++ % LIMITER_INTERVAL == 0 && !cancelled.get()) {
                listener.onFileFound(count, file.toPath());
            }

            return false;
        }

        @Override
        public boolean accept(File file, String s) {
            if (cancelled.get()) {
                error = new SearchManuallyAbortedException("manually aborted");
                throw new RuntimeException(error);
            }
            boolean isOk = availableExtensionList.contains(FileUtil.getExtension(file.getName()));
            if (isOk) {
                count++;
            }

            if (isOk && limiterCounter++ % LIMITER_INTERVAL == 0 && !cancelled.get()) {
                listener.onFileFound(count, file.toPath());
            }

            return false;
        }

    }


}
