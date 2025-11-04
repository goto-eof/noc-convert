package org.andreidodu.nocconvert.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class OperationUtil {
    private static final Logger log = LogManager.getLogger(FileUtil.class);

    private static final int MAX_DELETE_ATTEMPTS = 20;
    private static final long SLEEP_TIME_MS = 100;

    public static <T> void retryable(Semaphore vtSemaphore, T input, Function<T, Boolean> retryableOperation) {
        try {
            vtSemaphore.acquire();
            int attempt = 0;
            boolean isDone = false;

            while (!isDone && attempt < MAX_DELETE_ATTEMPTS) {
                isDone = retryableOperation.apply(input);
                attempt++;
                try {
                    Thread.sleep(SLEEP_TIME_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            log.error(getRootCauseMessage(e), e);
            throw new RuntimeException(e);
        } finally {
            vtSemaphore.release();
        }
    }
}
