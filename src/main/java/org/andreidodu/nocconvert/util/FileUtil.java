package org.andreidodu.nocconvert.util;

import java.io.File;

public class FileUtil {
    public static String getHumanableFileSize(File file) {
        if (file == null || !file.exists()) return "0 B";

        long bytes = file.length();
        if (bytes < 1024) return bytes + " B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = (int) (Math.log10(bytes) / Math.log10(1024));
        double size = bytes / Math.pow(1024, unitIndex);

        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
