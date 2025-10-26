package org.andreidodu.nocconvert.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileSystemService {
    List<String> getAllFiles(String directoryPath, List<String> allowedFileExtensionList) throws IOException;

    boolean containsAtLeaseOneFile(Path directoryPath);
}
