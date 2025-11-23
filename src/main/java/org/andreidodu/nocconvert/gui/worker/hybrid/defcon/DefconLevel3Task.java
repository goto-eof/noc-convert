package org.andreidodu.nocconvert.gui.worker.hybrid.defcon;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class DefconLevel3Task implements Runnable {
    private final CountDownLatch defconLevelLatch;
    private final int secondsToWait;
    private final Supplier<Boolean> isManualShutdown;

    public DefconLevel3Task(CountDownLatch defconLevelLatch, int secondsToWait, Supplier<Boolean> isManualShutdown) {
        this.defconLevelLatch = defconLevelLatch;
        this.secondsToWait = secondsToWait;
        this.isManualShutdown = isManualShutdown;
    }

    @Override
    public void run() {
        if (isManualShutdown.get()) {
            return;
        }
        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                defconLevelLatch.countDown();
                timer.cancel();
            }
        };

        timer.schedule(task, secondsToWait * 1000L, secondsToWait * 1000L);
    }
}
