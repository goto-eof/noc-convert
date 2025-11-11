package org.andreidodu.nocconvert.helper.performance;

import lombok.Getter;
import org.andreidodu.nocconvert.listener.AdaptiveTestListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.andreidodu.nocconvert.helper.CpuUtil.calculateCpuLoadPercentage;

public class AdaptiveSimpleGovernorRunnable {
    private static final Logger log = LogManager.getLogger(AdaptiveSimpleGovernorRunnable.class);

    public static final int NUM_PROCESSORS = Runtime.getRuntime().availableProcessors();
    public static final int INITIAL_DELAY = 2;
    private static final int MIN_NUM_CPUS_ALLOWED = 2;
    private static final int NUM_WARMUP_TESTS = 2;
    public static final int CPU_PERCENTAGE_BORDER_LINE = 80;
    private final AdaptiveTestListener adaptiveTestListener;
    @Getter
    private final AtomicInteger currentPTID = new AtomicInteger(NUM_PROCESSORS);
    public static final int DEFAULT_INITIAL_NUM_OF_THREADS = NUM_PROCESSORS;
    public final static AtomicInteger IDEAL_NUM_OF_THREADS = new AtomicInteger(DEFAULT_INITIAL_NUM_OF_THREADS);
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final Supplier<Boolean> isStopCommandFromParent;

    public AdaptiveSimpleGovernorRunnable(AdaptiveTestListener adaptiveTestListener, Supplier<Boolean> isStopCommandFromParent) {
        this.adaptiveTestListener = adaptiveTestListener;
        this.isStopCommandFromParent = isStopCommandFromParent;
    }

    public void runStressTest() {
        log.info("-----------------------");
        log.info("Stress Test Started");
        log.info("-----------------------");

        adaptiveTestListener.onOptimizationStart();
        log.info("cpus: {}", NUM_PROCESSORS);
        executeStressTestOnNewThread();
    }

    private void executeStressTestOnNewThread() {
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            executorService.submit(this::stressTestTask);
        }
    }

    private void stressTestTask() {
        log.debug("creating main threads");
        log.debug("creating watchdog thread");
        startScheduledService();
        log.debug("running stress test main thread");
        Thread.ofPlatform()
                .daemon(false)
                .start(this::startStressTestThreads);
    }


    private void startStressTestThreads() {
        try (ExecutorService executor = Executors.newCachedThreadPool()) {
            log.debug("Starting stress test threads");
            IntStream.range(0, NUM_PROCESSORS)
                    .forEach(index -> executor.submit(() -> intensiveTask(index)));
            log.debug("Completed test threads submission");
        }
    }

    private void startScheduledService() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> watchdogTask(scheduledExecutorService), INITIAL_DELAY, 1, TimeUnit.SECONDS);
    }

    private void watchdogTask(ScheduledExecutorService scheduledExecutorService) {
        log.debug("watchdog task for thread {}", currentPTID.get());
        currentPTID.set(currentPTID.get() - 1);
        if (!isStopCommandFromParent.get() && currentPTID.get() <= MIN_NUM_CPUS_ALLOWED) {
            IDEAL_NUM_OF_THREADS.set(MIN_NUM_CPUS_ALLOWED);
            cancel(scheduledExecutorService);
            log.debug("Canceled watchdog task because num. of threads is less than default");
            return;
        }

        int percentage = calculateCpuLoadPercentage();
        adaptiveTestListener.onOptimizationProgress("stress testing", currentPTID.get(), percentage);
        if (!isStopCommandFromParent.get() && wereDoneAtLeastXTests(NUM_WARMUP_TESTS) && percentage >= CPU_PERCENTAGE_BORDER_LINE - 10 && percentage <= CPU_PERCENTAGE_BORDER_LINE && isRunning.get()) {
            IDEAL_NUM_OF_THREADS.set(Integer.max(2, currentPTID.get()));
            log.info("Ideal number of threads is: {}", IDEAL_NUM_OF_THREADS);
        } else if (percentage < CPU_PERCENTAGE_BORDER_LINE) {
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
        while (isRunning.get() && i-- > 0 && currentPTID.get() > id && !isStopCommandFromParent.get()) ;
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
        return isRunning.get() && i < Double.MAX_VALUE && currentPTID.get() > id && !isStopCommandFromParent.get();
    }

    public static int calculateSafeValueWithoutXPercent(int permits, int percentage) {
        int result = (int) Math.ceil(permits * (1 - (double) percentage / 100));
        log.debug("Total permits: {}. Calculated permits of {} is {}", permits, percentage, result);
        return result;
    }

    public static int calculateValueWithXPercent(int permits, int percentage) {
        int result = (int) (permits * (1 + (double) percentage / 100));
        log.debug("Total permits: {}. Calculated permits of {} is {}", permits, percentage, result);
        return result;
    }

}
