package org.andreidodu.nocconvert.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class FileUtil {
    private static final Logger log = LogManager.getLogger(FileUtil.class);
    private static final int MAX_FILENAME_LENGTH = 150;

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

    public static Path getCleanFileNameWithoutExtension(Path inputPath) {
        String fileNameWithExtension = inputPath.getFileName().toString();

        int lastDotIndex = fileNameWithExtension.lastIndexOf('.');
        String baseName = (lastDotIndex == -1) ? fileNameWithExtension : fileNameWithExtension.substring(0, lastDotIndex);

        int firstDotIndex = baseName.indexOf('.');

        String cleanedName = baseName;
        if (firstDotIndex != -1) {
            cleanedName = baseName.substring(firstDotIndex + 1);
            log.debug("Removed UUID prefix based on first dot: {} -> {}", baseName, cleanedName);
        } else {
            log.debug("No UUID prefix found (no first dot in base name): {}", baseName);
        }

        cleanedName = cleanedName.replaceAll("[^a-zA-Z0-9\\s_.-]", "");
        cleanedName = cleanedName.trim().replaceAll("\\s+", "_");

        if (cleanedName.length() > MAX_FILENAME_LENGTH) {
            cleanedName = cleanedName.substring(0, MAX_FILENAME_LENGTH);
            log.warn("Truncated file name to {} characters: {}", MAX_FILENAME_LENGTH, cleanedName);
        }

        return Path.of(inputPath.getParent().toString(), cleanedName);
    }
}
