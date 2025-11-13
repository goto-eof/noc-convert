package org.andreidodu.nocconvert.task;

import org.andreidodu.nocconvert.helper.OSUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class AlwaysCleanTask implements Runnable {
    private static final Logger log = LogManager.getLogger(AlwaysCleanTask.class);

    private final BlockingQueue<Path> deletionQueue;

    public AlwaysCleanTask(final BlockingQueue<Path> deletionQueue) {
        this.deletionQueue = deletionQueue;
    }

    @Override
    public void run() {
        this.executeGlobalCleanup();
    }

    public void executeGlobalCleanup() {
        if (deletionQueue.isEmpty()) {
            log.info("deletion queue is empty. No cleanup required.");
            return;
        }

        List<Path> pathsToCleanup = new ArrayList<>();
        deletionQueue.drainTo(pathsToCleanup);

        List<String> command = buildCleanupCommand(pathsToCleanup);

        try {
            log.warn("Starting Always Clean Task (one-shot, fire-and-forget) for {} directories.", pathsToCleanup.size());
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

    private static List<String> buildCleanupCommand(List<Path> pathsToDelete) {
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
