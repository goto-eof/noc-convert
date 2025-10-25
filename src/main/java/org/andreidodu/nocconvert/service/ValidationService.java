package org.andreidodu.nocconvert.service;

import java.nio.file.Path;
import java.util.Optional;

public interface ValidationService {
    Optional<String> isValidatePath(Path path);
}
