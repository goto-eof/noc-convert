package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.service.ValidationService;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class ValidationServiceImpl implements ValidationService {

    @Override
    public Optional<String> isValidatePath(Path path) {
        if (!isValidaDirectory(path)) {
            return Optional.of("Invalid directory. Path does not exist or the path is not a directory");
        }
        return Optional.empty();
    }


    @Override
    public boolean isValidaDirectory(Path path) {
        if (path == null) {
            return false;
        }
        File file = path.toFile();
        return file.exists() && file.isDirectory();
    }

}
