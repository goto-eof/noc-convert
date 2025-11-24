package org.andreidodu.nocconvert.task;

import org.andreidodu.nocconvert.util.OSUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class AlwaysCleanTask implements Runnable {
    private static final Logger log = LogManager.getLogger(AlwaysCleanTask.class);

    private final Collection<Path> deletionCollection;

    public AlwaysCleanTask(final ConcurrentLinkedQueue<Path> deletionCollection) {
        this.deletionCollection = deletionCollection;
    }

    @Override
    public void run() {
        this.executeGlobalCleanup();
    }

    public void executeGlobalCleanup() {
        Collection<Path> collection = deletionCollection.stream().filter(Files::exists).collect(Collectors.toSet());
        if (collection.isEmpty()) {
            log.info("deletion queue is empty. No cleanup required.");
            return;
        }

        List<String> command = buildCleanupCommand(collection);

        try {
            log.warn("Starting Always Clean Task (one-shot, fire-and-forget) for {} directories.", deletionCollection.size());
            log.debug("Cleanup command: {}", command);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.start();
            log.warn("Always Clean Task command launched successfully. JVM will now exit.");

        } catch (IOException e) {
            log.error("Failed to execute  Always Clean Task command: {}", e.getMessage());
        }
    }

    private static List<String> buildCleanupCommand(Collection<Path> pathsToDelete) {
        List<String> command = new ArrayList<>();

        if (OSUtils.isWindows()) {
            command.add("cmd");
            command.add("/c");
            command.add("rd");
            command.add("/s");
            command.add("/q");
        } else {
            command.add("rm");
            command.add("-rf");
        }

        for (Path path : pathsToDelete) {
            command.add(path.toString());
        }

        return command;
    }

}
