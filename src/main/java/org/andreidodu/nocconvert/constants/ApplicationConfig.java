package org.andreidodu.nocconvert.constants;

public interface ApplicationConfig {

    boolean DEV_MODE = Boolean.parseBoolean(System.getProperty("dev.mode", "false"));

}
