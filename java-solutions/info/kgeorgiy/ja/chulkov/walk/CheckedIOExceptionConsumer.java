package info.kgeorgiy.ja.chulkov.walk;

import java.io.IOException;

@FunctionalInterface
public interface CheckedIOExceptionConsumer<T> {
    void accept(T t) throws IOException;
}
