package org.andreidodu.nocconvert.helper;

public class ThreadUtil {
    private static final long COOPERATIVE_SLEEP_MS = 10;

    public static boolean yieldCooperatively() {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        try {
            Thread.sleep(COOPERATIVE_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
        return false;
    }
}
