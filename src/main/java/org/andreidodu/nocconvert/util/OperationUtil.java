package org.andreidodu.nocconvert.util;

import org.andreidodu.nocconvert.funcinterface.ThrowingBiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

public class OperationUtil {
    private static final Logger log = LogManager.getLogger(FileUtil.class);

    private static final int MAX_ATTEMPTS = 20;
    private static final long SLEEP_TIME_MS = 100;

    public static boolean retryable(Callable<Boolean> operation) {
        try {
            int attempt = 0;
            boolean isDone = false;

            while (!isDone && attempt < MAX_ATTEMPTS) {
                try {
                    return operation.call();
                } catch (Exception ex) {
                    log.warn(ex.getMessage(), ex);
                }
                attempt++;
                sleep();
            }
            return false;
        } catch (Exception e) {
            log.error(getRootCauseMessage(e), e);
            throw new RuntimeException(e);
        }
    }

    public static <T, O> O retryable(Semaphore vtSemaphore, T input, Function<T, O> operation) {
        try {
            if (vtSemaphore != null) {
                vtSemaphore.acquire();
            }
            int attempt = 0;
            boolean isDone = false;

            while (!isDone && attempt < MAX_ATTEMPTS) {
                try {
                    return operation.apply(input);
                } catch (Exception ex) {
                    log.warn(ex.getMessage(), ex);
                }
                attempt++;
                sleep();
            }

            return null;

        } catch (Exception e) {
            log.error(getRootCauseMessage(e), e);
            throw new RuntimeException(e);
        } finally {
            if (vtSemaphore != null) {
                vtSemaphore.release();
            }
        }
    }

    public static <I1, I2> void retryable(Semaphore vtSemaphore, I1 input1, I2 input2, ThrowingBiConsumer<I1, I2> operation) {
        try {
            if (vtSemaphore != null) {
                vtSemaphore.acquire();
            }
            int attempt = 0;
            boolean isDone = false;

            while (!isDone && attempt < MAX_ATTEMPTS) {
                try {
                    operation.accept(input1, input2);
                } catch (Exception ex) {
                    log.warn(ex.getMessage(), ex);
                }
                attempt++;
                sleep();
            }
        } catch (Exception e) {
            log.error(getRootCauseMessage(e), e);
            throw new RuntimeException(e);
        } finally {
            if (vtSemaphore != null) {
                vtSemaphore.release();
            }
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(SLEEP_TIME_MS);
        } catch (InterruptedException e) {
            log.warn(e.getMessage(), e);
        }
    }
}
