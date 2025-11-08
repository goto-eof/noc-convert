package org.andreidodu.nocconvert.helper.performance;

import org.andreidodu.nocconvert.listener.AdaptiveTestListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class PerformanceUtil {
    private static final Logger log = LogManager.getLogger(PerformanceUtil.class);

    public static void checkPThreadPerformance(AdaptiveTestListener input, Supplier<Boolean> isStopCommandFromParent) {
        log.debug("checking CPU performance...");
        new AdaptiveSimpleGovernorRunnable(input, isStopCommandFromParent).runStressTest();
    }
}
