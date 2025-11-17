package org.andreidodu.nocconvert.helper;

import java.util.concurrent.ThreadFactory;

public class CustomVirtualThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        return Thread.ofVirtual().name("vt").unstarted(r);
    }
}
