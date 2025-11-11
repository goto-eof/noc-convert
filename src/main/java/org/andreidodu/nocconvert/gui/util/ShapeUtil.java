package org.andreidodu.nocconvert.gui.util;

import java.awt.geom.GeneralPath;

public class ShapeUtil {
    private static final float DEFAULT_ARC_SIZE = 5;

    public static GeneralPath getRoundedWestPath(int width, int height) {
        return getRoundedWestPath(width, height, DEFAULT_ARC_SIZE);
    }

    public static GeneralPath getRoundedEastPath(int width, int height) {
        return getRoundedEastPath(width, height, DEFAULT_ARC_SIZE);
    }

    public static GeneralPath getRoundedPath(int width, int height) {
        return getRoundedPath(width, height, DEFAULT_ARC_SIZE);
    }
    public static GeneralPath getRoundedWestPath(int width, int height, float arcSize) {
        GeneralPath path = new GeneralPath();
        path.moveTo(arcSize, 0);
        path.lineTo(width, 0);
        path.lineTo(width, height);
        path.lineTo(arcSize, height);
        path.quadTo(0, height, 0, height - arcSize);
        path.lineTo(0, arcSize);
        path.quadTo(0, 0, arcSize, 0);
        path.closePath();
        return path;
    }

    public static GeneralPath getRoundedEastPath(int width, int height, float arcSize) {
        GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);
        path.lineTo(width - arcSize, 0);
        path.quadTo(width, 0, width, arcSize);
        path.lineTo(width, height - arcSize);
        path.quadTo(width, height, width - arcSize, height);
        path.lineTo(0, height);
        path.lineTo(0, 0);
        path.closePath();
        return path;
    }

    public static GeneralPath getRoundedPath(int width, int height, float arcSize) {
        GeneralPath path = new GeneralPath();
        path.moveTo(arcSize, 0);
        path.lineTo(width - arcSize, 0);
        path.quadTo(width, 0, width, arcSize);
        path.lineTo(width, height - arcSize);
        path.quadTo(width, height, width - arcSize, height);

        path.lineTo(arcSize, height);
        path.quadTo(0, height, 0, height - arcSize);
        path.lineTo(0, arcSize);
        path.quadTo(0, 0, arcSize, 0);
        path.closePath();
        return path;
    }
}
