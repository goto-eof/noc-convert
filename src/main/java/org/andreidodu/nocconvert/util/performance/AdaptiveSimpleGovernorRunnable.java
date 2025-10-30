package org.andreidodu.nocconvert.util.performance;

import lombok.Getter;
import org.andreidodu.nocconvert.listener.AdaptiveTestListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.*;
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
    public static final int DEFAULT_INITIAL_NUM_OF_THREADS = NUM_PROCESSORS;
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
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            executorService.submit(this::stressTestTask);
        }
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
            log.debug("Starting stress test threads");
            IntStream.range(0, NUM_PROCESSORS)
                    .forEach(index -> executor.submit(() -> intensiveTask(index)));
            log.debug("Completed test threads submission");
        }
    }

    private void startScheduledService(com.sun.management.OperatingSystemMXBean osBean) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> watchdogTask(osBean, scheduledExecutorService), INITIAL_DELAY, 1, TimeUnit.SECONDS);
    }

    private void watchdogTask(com.sun.management.OperatingSystemMXBean osBean, ScheduledExecutorService scheduledExecutorService) {
        log.debug("watchdog task for thread {}", currentPTID.get());
        currentPTID.set(currentPTID.get() - 1);
        if (currentPTID.get() <= 2) {
            cancel(scheduledExecutorService);
            log.debug("Canceled watchdog task because num. of threads is less than default");
            return;
        }

        double cpuLoad = osBean.getCpuLoad();
        int percentage = (int) (cpuLoad * 100);
        adaptiveTestListener.onOptimizationProgress("stress testing", currentPTID.get(), percentage);
        if (wereDoneAtLeastXTests(2) && percentage >= 80 && percentage <= 90.0 && isRunning.get()) {
            IDEAL_NUM_OF_THREADS.set(Integer.max(IDEAL_NUM_OF_THREADS.get(), currentPTID.get()));
            log.info("Ideal number of threads is: {}", IDEAL_NUM_OF_THREADS);
        } else if (percentage < 80) {
            log.debug("Canceled watchdog task because percentage is less than 80");
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
        log.debug("Starting intensive task for id {}", id);

        FutureTask<Boolean> future = new FutureTask<>(() -> task(id));
        Thread.ofPlatform().start(future);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getCause().getMessage(), e.getCause());
            throw new RuntimeException(e);
        }
        log.debug("Submitted intensive pt for id {}", id);

        sleep();
        lessIntensiveTask(id);
        sleep();
        lessIntensiveTask(id);
        sleep();
        lessIntensiveTask(id);
        log.debug("Finished intensive task for id {}", id);


    }

    private void lessIntensiveTask(int id) {
        int i = 1000000;
        while (isRunning.get() && i-- > 0 && currentPTID.get() > id) ;
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean task(int id) {
        double i = 0;
        while (isShouldRun(id, i)) {
            i = updateIncrementor(i);
        }
        return true;
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
