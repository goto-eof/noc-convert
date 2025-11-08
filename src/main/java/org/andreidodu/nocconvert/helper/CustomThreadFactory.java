package org.andreidodu.nocconvert.helper;

import java.util.concurrent.ThreadFactory;

public class CustomThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("noc-convert-thread-" + t.getId());
        t.setDaemon(true);
        return t;
    }
}
