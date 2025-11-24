package org.andreidodu.nocconvert.enums;

import lombok.Getter;

public enum DEFCON {
    LEVEL_1(1, 90), LEVEL_2(2, 80), LEVEL_3(3, 70);
    @Getter
    private final int level;

    @Getter
    private final int criticalPoint;

    DEFCON(int level, int criticalPoint) {
        this.level = level;
        this.criticalPoint = criticalPoint;
    }
}
