package org.andreidodu.nocconvert.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

public class EDTLogger {
    private static final Logger EDT_LOG = LogManager.getLogger("EDT_ACTIVITY");
    private static final long WARNING_THRESHOLD_MS = 50;

    public static void logEDTActivity(String label, Runnable runnable) {
        SwingUtilities.invokeLater(() -> {
            long startTime = System.currentTimeMillis();
            try {
                runnable.run();
            } catch (Exception e) {
                EDT_LOG.error("Exception occurred during EDT execution for: " + label, e);
            } finally {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                if (duration > 5 || duration >= WARNING_THRESHOLD_MS) {
                    EDT_LOG.warn("EDT Task '{}' finished in {} ms. WARNING: >{}ms indicates a block.",
                            label, duration, WARNING_THRESHOLD_MS);
                } else {
                    EDT_LOG.debug("EDT Task '{}' finished in {} ms.", label, duration);
                }
            }
        });
    }
}
