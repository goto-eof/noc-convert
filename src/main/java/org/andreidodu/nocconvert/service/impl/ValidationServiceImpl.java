package org.andreidodu.nocconvert.service.impl;

import org.andreidodu.nocconvert.service.ValidationService;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class ValidationServiceImpl implements ValidationService {

    @Override
    public Optional<String> isValidatePath(Path path) {
        if (!isValidaDirectory(path)) {
            return Optional.of(String.format("Path %s does not exist or is not a directory", path.toString()));
        }
        return Optional.empty();
    }


    private boolean isValidaDirectory(Path path) {
        if (path == null) {
            return false;
        }
        File file = path.toFile();
        return file.exists() && file.isDirectory();
    }

}
