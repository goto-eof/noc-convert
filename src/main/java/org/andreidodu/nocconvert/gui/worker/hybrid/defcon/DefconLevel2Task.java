package org.andreidodu.nocconvert.gui.worker.hybrid.defcon;

import org.andreidodu.nocconvert.util.RamUtil;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

public class DefconLevel2Task implements Runnable {
    private final RamUtil ramUtil;
    private final CountDownLatch defconLevelLatch;
    private final static int TIC_TOC_SECONDS = 15;
    private final Supplier<Boolean> isManualShutdown;

    public DefconLevel2Task(CountDownLatch defconLevelLatch, Supplier<Boolean> isManualShutdown) {
        ramUtil = new RamUtil();
        this.defconLevelLatch = defconLevelLatch;
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

        timer.scheduleAtFixedRate(task, TIC_TOC_SECONDS * 1000, TIC_TOC_SECONDS * 1000);
    }
}
