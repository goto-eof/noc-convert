package org.andreidodu.nocconvert.helper;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class CpuUtil {


    public static int calculateCpuLoadPercentage() {
        double cpuLoad = getOperatingSystemMXBean().getCpuLoad();
        return (int) (cpuLoad * 100);
    }

    public static com.sun.management.OperatingSystemMXBean getOperatingSystemMXBean() {
        OperatingSystemMXBean modernOsBean = ManagementFactory.getOperatingSystemMXBean();
        return (com.sun.management.OperatingSystemMXBean) modernOsBean;
    }
}
