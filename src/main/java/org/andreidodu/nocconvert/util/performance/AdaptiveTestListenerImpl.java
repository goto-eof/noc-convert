package org.andreidodu.nocconvert.util.performance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdaptiveTestListenerImpl implements AdaptiveGovernorRunnable.AdaptiveTestListener {
    private static final Logger log = LogManager.getLogger(AdaptiveTestListenerImpl.class);

    @Override
    public void onOptimizationStart() {
        log.debug("Calculation of optimal number of V-Thread test started...");
    }

    @Override
    public void onOptimizationProgress(String statusMessage, int currentLimit, double cpuLoad) {
        log.debug(statusMessage, currentLimit, cpuLoad);
    }

    @Override
    public void onOptimizationComplete(int finalOptimalLimit) {
        log.info("Your system supports maximum  " + finalOptimalLimit + " V-Threads");
    }
}
