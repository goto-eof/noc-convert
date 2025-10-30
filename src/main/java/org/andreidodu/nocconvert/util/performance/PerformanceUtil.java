package org.andreidodu.nocconvert.util.performance;

import org.andreidodu.nocconvert.listener.AdaptiveTestListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.FutureTask;

public class PerformanceUtil {
    private static final Logger log = LogManager.getLogger(PerformanceUtil.class);

    public static void checkVThreadPerformance(AdaptiveTestListener input) {

        FutureTask<?> taskFuture = new FutureTask<>(() -> {
            new AdaptiveSimpleGovernorRunnable(input).run();
            return null;
        });

        Thread outerThread = new Thread(() -> {
            log.info("Starting performance testing...");

            Thread innerThread = new Thread(taskFuture, "performance-tester");
            innerThread.setDaemon(true);
            innerThread.start();
            try {
                innerThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "performance-tester-watchdog");
        outerThread.setDaemon(true);
        outerThread.start();
    }
}
