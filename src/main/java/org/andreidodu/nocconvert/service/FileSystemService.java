package org.andreidodu.nocconvert.service;

import java.io.IOException;
import java.util.List;

public interface FileSystemService {
    List<String> getAllFiles(String directoryPath, List<String> allowedFileExtensionList) throws IOException;

    boolean containsAtLeaseOneFile(String directoryPath);
}
