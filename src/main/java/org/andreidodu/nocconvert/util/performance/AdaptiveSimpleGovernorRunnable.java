package org.andreidodu.nocconvert.util.performance;

import lombok.Getter;
import org.andreidodu.nocconvert.listener.AdaptiveTestListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class AdaptiveSimpleGovernorRunnable {
    private static final Logger log = LogManager.getLogger(AdaptiveSimpleGovernorRunnable.class);

    public static final int NUM_PROCESSORS = Runtime.getRuntime().availableProcessors();
    public static final int INITIAL_DELAY = 2;
    private final AdaptiveTestListener adaptiveTestListener;
    @Getter
    private final AtomicInteger currentPTID = new AtomicInteger(NUM_PROCESSORS);
    public static final int DEFAULT_INITIAL_NUM_OF_THREADS = 2;
    public final static AtomicInteger IDEAL_NUM_OF_THREADS = new AtomicInteger(DEFAULT_INITIAL_NUM_OF_THREADS);
    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public AdaptiveSimpleGovernorRunnable(AdaptiveTestListener adaptiveTestListener) {
        this.adaptiveTestListener = adaptiveTestListener;
    }

    public void runStressTest() {
        log.info("-----------------------");
        log.info("Stress Test Started");
        log.info("-----------------------");

        adaptiveTestListener.onOptimizationStart();
        executeStressTestOnNewThread();
    }

    private void executeStressTestOnNewThread() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::stressTestTask);
        executorService.shutdown();
    }

    private void stressTestTask() {
        com.sun.management.OperatingSystemMXBean osBean = getOperatingSystemMXBean();
        startScheduledService(osBean);
        Thread.ofPlatform().start(this::startStressTestThreads);
    }

    private static com.sun.management.OperatingSystemMXBean getOperatingSystemMXBean() {
        OperatingSystemMXBean modernOsBean = ManagementFactory.getOperatingSystemMXBean();
        return (com.sun.management.OperatingSystemMXBean) modernOsBean;
    }

    private void startStressTestThreads() {
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            IntStream.range(0, NUM_PROCESSORS)
                    .forEach(index -> executor.submit(() -> intensiveTask(index)));
        }
    }

    private void startScheduledService(com.sun.management.OperatingSystemMXBean osBean) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> watchdogTask(osBean, scheduledExecutorService), INITIAL_DELAY, 1, TimeUnit.SECONDS);
    }

    private void watchdogTask(com.sun.management.OperatingSystemMXBean osBean, ScheduledExecutorService scheduledExecutorService) {
        currentPTID.set(currentPTID.get() - 1);
        if (currentPTID.get() <= DEFAULT_INITIAL_NUM_OF_THREADS) {
            cancel(scheduledExecutorService);
            return;
        }

        double cpuLoad = osBean.getCpuLoad();
        int percentage = (int) (cpuLoad * 100);
        adaptiveTestListener.onOptimizationProgress("stress testing", currentPTID.get(), percentage);
        //
        if (wereDoneAtLeastXTests(2) && percentage >= 80 && percentage <= 90.0 && isRunning.get()) {
            IDEAL_NUM_OF_THREADS.set(Integer.max(IDEAL_NUM_OF_THREADS.get(), currentPTID.get()));
            log.info("Ideal number of threads is: {}", IDEAL_NUM_OF_THREADS);
        } else if (percentage < 80) {
            cancel(scheduledExecutorService);
        }
    }

    private boolean wereDoneAtLeastXTests(int x) {
        return currentPTID.get() < NUM_PROCESSORS - x;
    }

    private void cancel(ScheduledExecutorService scheduledExecutorService) {
        isRunning.set(false);
        adaptiveTestListener.onOptimizationComplete(IDEAL_NUM_OF_THREADS.get());
        scheduledExecutorService.shutdownNow();
    }

    private void intensiveTask(int id) {
        double i = 0;
        while (isShouldRun(id, i)) {
            i = updateIncrementor(i);
        }
    }

    private static double updateIncrementor(double i) {
        i++;
        if (i == Double.MAX_VALUE) {
            i = 0;
        }
        return i;
    }

    private boolean isShouldRun(int id, double i) {
        return isRunning.get() && i < Double.MAX_VALUE && currentPTID.get() > id;
    }

}
