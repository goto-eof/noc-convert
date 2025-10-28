package org.andreidodu.nocconvert.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class FileUtil {
    private static final Logger log = LogManager.getLogger(FileUtil.class);

    public static String getHumanableFileSize(File file) {
        if (file == null || !file.exists()) return "0 B";

        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = (int) (Math.log10(bytes) / Math.log10(1024));
        double size = bytes / Math.pow(1024, unitIndex);

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    public static void deleteBrokenFileIfExists(File outputFile) {
        if (outputFile.exists()) {
            if (outputFile.delete()) {
                log.debug("'Partial file' removed: {}", outputFile.getName());
            } else {
                log.warn("Unable to remove the 'partial file': {}", outputFile.getName());
            }
        }
    }
}
