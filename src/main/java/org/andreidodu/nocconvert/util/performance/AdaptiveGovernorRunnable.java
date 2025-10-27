package org.andreidodu.nocconvert.util.performance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AdaptiveGovernorRunnable implements Runnable {
    private static final Logger log = LogManager.getLogger(AdaptiveGovernorRunnable.class);
    public static final int MAX_PRIME = 5_000;

    public static volatile int VT_OPTIMAL_LIMIT = Runtime.getRuntime().availableProcessors();

    private static final int NUM_CPU_CORES = Runtime.getRuntime().availableProcessors() / 2;
    private static final double TARGET_CPU_MAX = 0.85;
    private static final int MAX_PERMITS_CAP = NUM_CPU_CORES * 50;
    private static final int ADJUSTMENT_STEP = Math.max(1, NUM_CPU_CORES / 2);
    private static final long TEST_DURATION_SECONDS = 10;

    private final AdaptiveTestListener listener;
    private volatile Thread submissionThread;

    public interface AdaptiveTestListener {
        void onOptimizationStart();

        void onOptimizationProgress(String statusMessage, int currentLimit, double cpuLoad);

        void onOptimizationComplete(int finalOptimalLimit);
    }

    public AdaptiveGovernorRunnable(AdaptiveTestListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        if (listener != null) {
            listener.onOptimizationStart();
        }

        try {
            log.debug("----------------------------------------------------------------");
            log.debug("Starting Device Performance Optimization...");
            log.debug("----------------------------------------------------------------");

            int finalLimit = runGovernorTest(TEST_DURATION_SECONDS);

            VT_OPTIMAL_LIMIT = finalLimit;

            System.out.printf("OPTIMIZATION COMPLETE. Limit set to: %d\n", VT_OPTIMAL_LIMIT);

            if (listener != null) {
                listener.onOptimizationComplete(VT_OPTIMAL_LIMIT);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Optimization interrupted.");
        }
    }

    private int runGovernorTest(long durationSeconds) throws InterruptedException {

        Semaphore adaptiveSemaphore = new Semaphore(NUM_CPU_CORES * 2);
        AtomicInteger currentLimit = new AtomicInteger(NUM_CPU_CORES * 2);

        java.util.concurrent.ExecutorService vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        startAdaptiveController(scheduler, adaptiveSemaphore, currentLimit);
        startTaskSubmission(vThreadExecutor, adaptiveSemaphore);

        Thread.sleep(durationSeconds * 1000L);

        if (submissionThread != null) {
            submissionThread.interrupt();
        }

        scheduler.shutdownNow();
        vThreadExecutor.shutdownNow();

        try {
            if (!vThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("V-Thread Executor was not terminated.");
            }
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Scheduler not termintede.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return currentLimit.get();
    }

    private void startAdaptiveController(
            ScheduledExecutorService scheduler,
            Semaphore adaptiveSemaphore,
            AtomicInteger currentLimit) {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        scheduler.scheduleAtFixedRate(() -> {

            double cpuLoad = 0.0;
            try {
                cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            int currentEffectivePermits = currentLimit.get();
            String loadStatus;

            if (cpuLoad < TARGET_CPU_MAX - 0.10) {
                int newLimit = Math.min(MAX_PERMITS_CAP, currentEffectivePermits + ADJUSTMENT_STEP);
                if (newLimit > currentEffectivePermits) {
                    int toRelease = newLimit - currentEffectivePermits;
                    adaptiveSemaphore.release(toRelease);
                    currentLimit.set(newLimit);
                    loadStatus = "Scaling up";
                } else {
                    loadStatus = "Max limit reached";
                }

            } else if (cpuLoad > TARGET_CPU_MAX + 0.05) {
                int newLimit = Math.max(NUM_CPU_CORES, currentEffectivePermits - ADJUSTMENT_STEP);

                if (newLimit < currentEffectivePermits) {
                    int toAcquire = currentEffectivePermits - newLimit;
                    adaptiveSemaphore.tryAcquire(toAcquire);
                    currentLimit.set(newLimit);
                    loadStatus = "Scaling down";
                } else {
                    if (cpuLoad > TARGET_CPU_MAX + 0.05) {
                        loadStatus = "Overloaded (Min Limit)";
                    } else {
                        loadStatus = "Min limit reached";
                    }
                }
            } else {
                loadStatus = "Stable";
            }

            if (listener != null) {
                listener.onOptimizationProgress(loadStatus, currentLimit.get(), cpuLoad * 100);
            }

        }, 0, 2, TimeUnit.SECONDS);
    }

    private void startTaskSubmission(
            java.util.concurrent.ExecutorService vThreadExecutor,
            Semaphore adaptiveSemaphore) {
        submissionThread = Thread.ofVirtual().start(() -> {
            while (!Thread.currentThread().isInterrupted()) {

                try {
                    vThreadExecutor.submit(() -> {
                        Random random = new Random();
                        isPrime(random.nextInt(MAX_PRIME));
                        processTestWorkload(adaptiveSemaphore);
                        isPrime(random.nextInt(MAX_PRIME));
                    });
                } catch (java.util.concurrent.RejectedExecutionException e) {
                    System.err.println("Task submission rejected: Executor is shutdown. Stopping submission loop.");
                    Thread.currentThread().interrupt();
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void processTestWorkload(Semaphore adaptiveSemaphore) {
        try {
            adaptiveSemaphore.acquire();

            long startTime = System.nanoTime();
            long maxDurationNanos = 30_000_000 + new Random().nextInt(10_000_000);

            Random random = new Random();
            while (System.nanoTime() - startTime < maxDurationNanos) {
                isPrime(random.nextInt(MAX_PRIME));
            }

            Thread.sleep(5);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (adaptiveSemaphore.availablePermits() < MAX_PERMITS_CAP) {
                adaptiveSemaphore.release();
            }
        }
    }

    private boolean isPrime(long n) {
        if (n <= 1) {
            return false;
        }
        for (int i = 2; i < n; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }
}
