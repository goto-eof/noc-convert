package org.andreidodu.nocconvert.funcinterface;

import java.io.IOException;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U> {
    void accept(T t, U u) throws IOException;
}
