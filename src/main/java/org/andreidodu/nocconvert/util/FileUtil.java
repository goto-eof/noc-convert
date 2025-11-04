package org.andreidodu.nocconvert.util;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

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


    private static boolean deleteFile(File outputFile) {
        if (outputFile.delete()) {
            log.debug("'Partial file' removed: {}", outputFile.getName());
            return true;
        } else {
            log.warn("Unable to remove the 'partial file': {}", outputFile.getName());
            return false;
        }
    }

    public static Path getCleanFileNameWithoutExtension(Path inputPath) {
        String fileNameWithExtension = inputPath.getFileName().toString();
        int firstDotIndex = fileNameWithExtension.indexOf('.');

        String extractedName;

        if (firstDotIndex != -1) {
            extractedName = fileNameWithExtension.substring(firstDotIndex + 1);
            log.debug("Removed UUID prefix: {} -> {}", fileNameWithExtension, extractedName);
        } else {
            extractedName = fileNameWithExtension;
            log.debug("No dot found, keeping name: {}", extractedName);
        }

        String sanitizedName = extractedName
                .replaceAll("[^a-zA-Z0-9\\s_.-]", "")
                .trim()
                .replaceAll("\\s+", "_");

        int lastDotIndex = sanitizedName.lastIndexOf('.');
        String baseName = sanitizedName;
        String extension = "";

        if (lastDotIndex != -1) {
            baseName = sanitizedName.substring(0, lastDotIndex);
            extension = sanitizedName.substring(lastDotIndex);
        }

        if (baseName.length() > MAX_FILENAME_LENGTH) {
            baseName = baseName.substring(0, MAX_FILENAME_LENGTH);
            log.warn("Filename base truncated to {} characters: {}", MAX_FILENAME_LENGTH, baseName);
        }

        String finalName = baseName + extension;

        if (finalName.isEmpty()) {
            log.error("Sanitized name resulted in empty string. Using fallback name.");
            finalName = "clean_file_fallback";
            if (!extension.isEmpty()) {
                finalName += extension;
            }
        }

        Path parentDir = inputPath.getParent();

        if (parentDir != null) {
            return parentDir.resolve(finalName);
        } else {
            return Path.of(finalName);
        }
    }

    public static boolean deleteFileIfExists(File outputFile) {
        if (!outputFile.exists() || !outputFile.isFile()) {
            return true;
        }
        return deleteFile(outputFile);
    }

    public static void deleteTemporaryDirectory(boolean isPathExists, Path toDeletePath) {
        if (isPathExists) {
            deleteDirectory(toDeletePath);
        }
    }

    public static boolean deleteDirectory(Path toDeletePath) {
        try {
            File file = toDeletePath.toFile();
            if (!file.exists() || !file.isDirectory()) {
                return true;
            }
            FileUtils.deleteDirectory(file);
            return true;
        } catch (IOException e) {
            log.debug(getRootCauseMessage(e), e);
            return false;
        }
    }
}
