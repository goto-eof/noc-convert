package service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface FileSystemService {
    List<String> getAllFiles(String directoryPath, List<String> allowedFileExtensionList) throws IOException;

    boolean containsAtLeaseOneFile(String directoryPath);
}
