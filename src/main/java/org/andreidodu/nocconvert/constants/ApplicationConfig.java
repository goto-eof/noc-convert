package org.andreidodu.nocconvert.constants;

public class ApplicationConfig {

    public static boolean DEV_MODE = Boolean.parseBoolean(System.getProperty("dev.mode", "false"));
    public static final String FINAL_OUTPUT_DIRECTORY_NAME = "noc-convert";
    public static final String TMP_OUTPUT_DIRECTORY_NAME = "noc-convert-tmp";
    public static final int NUMBER_OF_FILES_PER_DIRECTORY = 10_000;
    public static final int MINIMUM_BUCKET_SIZE = 10;
    public static final int DEFCON_3_MAX_SECONDS_TO_WAIT = 10;
    public static final int MAX_BUCKET_SIZE = 1_000;

    public static void reloadDevMode() {
        ApplicationConfig.DEV_MODE = Boolean.parseBoolean(System.getProperty("dev.mode", "false"));
    }

}
