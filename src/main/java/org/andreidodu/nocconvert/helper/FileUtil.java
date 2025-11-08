package org.andreidodu.nocconvert.helper;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class FileUtil {
    private static final Logger log = LogManager.getLogger(FileUtil.class);
    private static final int MAX_FILENAME_LENGTH = 150;
    private static final AtomicLong fileID = new AtomicLong(0);
    private static final Object RENAME_LOCK = new Object();

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

    public static Path getCleanFileNameW(Path inputPath, String targetExtension) {
        String fileNameWithExtension = inputPath.getFileName().toString();
        int firstDotIndex = fileNameWithExtension.indexOf('.');

        String extractedName;

        int lengthUUID = UUID.randomUUID().toString().length();
        if (firstDotIndex != -1 && lengthUUID == fileNameWithExtension.substring(0, firstDotIndex).length()) {
            extractedName = fileNameWithExtension.substring(firstDotIndex + 1);
            log.debug("Removed UUID prefix: {} -> {}", fileNameWithExtension, extractedName);
        } else {
            extractedName = fileNameWithExtension;
        }
        log.debug("No dot found, keeping name: {}", extractedName);

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

    private static long countCharInString(char c, String fileNameWithExtension) {
        return fileNameWithExtension.chars().filter(ch -> ch == c).count();
    }

    public static boolean deleteFileIfExists(File outputFile) {
        if (!outputFile.exists() || !outputFile.isFile()) {
            return true;
        }
        return deleteFile(outputFile);
    }

    public static boolean deleteDirectoryIfExists(Path toDeletePath) {
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


    public static boolean renameFile(Path file, String targetExtension) {
        synchronized (RENAME_LOCK) {
            // Path clean = getCleanFileNameWithoutExtension(file);
            // Path nonExistingFile = file;
            Path nonExistingFile = calculateANonExistingFileName(file, targetExtension);

            if (nonExistingFile.toFile().exists()) {
            }
            try {
                Files.move(file, nonExistingFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error(getRootCauseMessage(e), e);
                return false;
            }
            return true;
        }
    }

    public static Path calculateANonExistingFileName(Path originalOutputFile, String targetExtension) {

        Path file = originalOutputFile;
        while (Files.exists(file)) {
            String cleanFileName = FileUtil.getCleanFileNameW(originalOutputFile.getFileName(), targetExtension).toString();
            file = calculateNewName(originalOutputFile, fileID.incrementAndGet(), cleanFileName, targetExtension);
        }

        return file;
    }

    private static Path calculateNewName(Path originlPath, long fileID, String cleanFilename, String targetExtension) {
        Path newPath = Path.of(originlPath.getParent().toString(), cleanFilename);
        if (!newPath.toFile().exists()) {
            return newPath;
        }

        String calculatedFilename = insertIdAndGet(cleanFilename, fileID, targetExtension);
        return Path.of(originlPath.getParent().toString(), calculatedFilename);
    }

    private static String insertIdAndGet(String filename, long fileID, String targetExtensionWithouthDot) {
        String targetExtension = targetExtensionWithouthDot;
        if (targetExtension.indexOf('.') == -1) {
            targetExtension = "." + targetExtension;
        }

        int lastDotIndex = filename.lastIndexOf('.');

        if (lastDotIndex != -1) {
            String onlyName = filename.substring(0, lastDotIndex);
            String dotWithOldExtension = filename.substring(lastDotIndex);

            if (onlyName.trim().isEmpty() && !dotWithOldExtension.trim().isEmpty()) {
                return "file-id-" + fileID + targetExtension;
            }
            if (onlyName.trim().isEmpty() && dotWithOldExtension.trim().isEmpty()) {
                return "file-id-" + fileID + targetExtension;
            }
            if (!onlyName.trim().isEmpty() && dotWithOldExtension.trim().isEmpty()) {
                return onlyName + ".duplicate-id-" + fileID + targetExtension;
            }

            if (!onlyName.trim().isEmpty() && !dotWithOldExtension.trim().isEmpty()) {
                return onlyName + ".duplicate-id-" + fileID + dotWithOldExtension;
            }
        }
        return filename + ".duplicate-id-" + fileID + targetExtension;


//        if (!filename.contains(".")) {
//            return "duplicate-id-" + fileID + "-" + filename + "." + targetExtension;
//        }
//        if (countCharInString('.', filename) == 1) {
//            return filename.substring(0, lastDotIndex) + 1 + "duplicate-id-" + fileID + filename.substring(lastDotIndex);
//        }
//        return filename.substring(0, lastDotIndex + 1) + "duplicate-id-" + fileID + filename.substring(lastDotIndex);
    }
}
