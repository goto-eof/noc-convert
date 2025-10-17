package andreidodu.nocconvert.util;

import java.util.concurrent.ThreadFactory;

public class CustomThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("image-converter");
        t.setDaemon(true);
        return t;
    }
}
