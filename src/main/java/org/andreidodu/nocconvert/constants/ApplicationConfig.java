package org.andreidodu.nocconvert.constants;

public class ApplicationConfig {

    public static boolean DEV_MODE = Boolean.parseBoolean(System.getProperty("dev.mode", "false"));

    public static void reloadDevMode() {
        ApplicationConfig.DEV_MODE = Boolean.parseBoolean(System.getProperty("dev.mode", "false"));
    }

}
