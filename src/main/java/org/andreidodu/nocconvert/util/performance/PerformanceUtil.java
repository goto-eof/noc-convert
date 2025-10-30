package org.andreidodu.nocconvert.util.performance;

import org.andreidodu.nocconvert.listener.AdaptiveTestListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PerformanceUtil {
    private static final Logger log = LogManager.getLogger(PerformanceUtil.class);

    public static void checkVThreadPerformance(AdaptiveTestListener input) {
        new AdaptiveSimpleGovernorRunnable(input).runStressTest();
    }
}
