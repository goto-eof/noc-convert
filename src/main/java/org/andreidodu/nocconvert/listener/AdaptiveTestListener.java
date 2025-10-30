package org.andreidodu.nocconvert.listener;

public interface AdaptiveTestListener {
    void onOptimizationStart();

    void onOptimizationProgress(String statusMessage, int currentLimit, double cpuLoad);

    void onOptimizationComplete(int finalOptimalLimit);
}
