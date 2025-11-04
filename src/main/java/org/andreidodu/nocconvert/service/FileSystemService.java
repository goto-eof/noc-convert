package org.andreidodu.nocconvert.service;

import java.nio.file.Path;

public interface FileSystemService {

    boolean containsAtLeaseOneFile(Path directoryPath);

}
