package org.andreidodu.nocconvert.constants;

public class ApplicationConfig {

    public static boolean DEV_MODE = Boolean.parseBoolean(System.getProperty("dev.mode", "false"));
    public static final String FINAL_OUTPUT_DIRECTORY_NAME = "noc-convert";
    public static final String TMP_OUTPUT_DIRECTORY_NAME = "noc-convert-tmp";

    public static void reloadDevMode() {
        ApplicationConfig.DEV_MODE = Boolean.parseBoolean(System.getProperty("dev.mode", "false"));
    }

}
