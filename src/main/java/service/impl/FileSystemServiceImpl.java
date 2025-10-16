package service.impl;

import service.FileSystemService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSystemServiceImpl implements FileSystemService {

    @Override
    public List<String> getAllFiles(String directoryPath, List<String> allowedFileExtensionList) {
        Path start = Paths.get(directoryPath);
        try (Stream<Path> pathStream = Files.walk(start)) {
            return pathStream.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(string -> {
                        String name = string.toLowerCase();
                        return allowedFileExtensionList.stream()
                                .anyMatch(name::endsWith);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
