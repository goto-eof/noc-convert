package org.andreidodu.nocconvert.util;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class RamUtil {
    private static HardwareAbstractionLayer hal;

    public HardwareAbstractionLayer getHal() {
        if (hal == null) {
            SystemInfo si = new SystemInfo();
            hal = si.getHardware();
        }
        return hal;
    }

    public double getConsumedRam() {
        return (getHal().getMemory().getTotal() - (double) getHal().getMemory().getAvailable()) / 1024 / 1024;
    }

    public double getMaxRam() {
        return (double) getHal().getMemory().getTotal() / 1024 / 1024;
    }

    public int getCurrentOccupiedRAMPercentage() {
        GlobalMemory memory = getHal().getMemory();
        long total = memory.getTotal();
        long onePerc = total / 100;
        long available = memory.getAvailable();
        return (int) ((total - available) / onePerc);
    }
}
