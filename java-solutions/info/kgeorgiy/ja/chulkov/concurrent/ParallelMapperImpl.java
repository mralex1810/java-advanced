package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Implementation of {@link ParallelMapper}
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final SynchronizedQueue<Task<?>> tasks = new SynchronizedQueue<>();
    private final List<Thread> threads;
    private volatile boolean closed = false;

    /**
     * Constructs new {@code ParallelMapperImpl} by number of workers threads.
     *
     * @param threadsNum number of thread workers in constructing {@code ParallelMapperImpl}
     */
    public ParallelMapperImpl(final int threadsNum) {
        IterativeParallelism.checkThreads(threadsNum);

        final Runnable worker = () -> {
            try {
                while (!Thread.interrupted()) {
                    tasks.take().runnable.run();
                }
            } catch (final InterruptedException ignored) {
            }
        };

        // :NOTE: (Int)Stream
        this.threads = IntStream.range(0, threadsNum)
                .mapToObj(it -> new Thread(worker))
                .peek(Thread::start)
                .toList();
    }

    public <T, R> List<R> map(
            final Function<? super T, ? extends R> f,
            final List<? extends T> args
    ) throws InterruptedException {
        checkClosed();
        final Results<R> results = new Results<>(args.size());
        tasks.putAll(IntStream.range(0, args.size())
                .<Task<?>>mapToObj(index -> new Task<>(() -> {
                    try {
                        results.setResult(index, f.apply(args.get(index)));
                    } catch (final RuntimeException e) {
                        // :NOTE: RuntimeException
                        results.setException(e);
                    }
                }, results))
                .toList()
        );
        return results.getResults();
    }

    @Override
    public void close() {
        // :NOTE: hang map
        closed = true;
        threads.forEach(Thread::interrupt);
        tasks.forEach(it -> {
            synchronized (it.result) {
                it.result.notify();
            }
        });
        for (final Thread thread : threads) {
            boolean joined = false;
            while (!joined) {
                try {
                    thread.join();
                    joined = true;
                } catch (final InterruptedException ignored) {
                }
            }
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Mapper was closed");
        }
    }

    private record Task<R>(Runnable runnable, Results<R> result) {

    }

    private static class SynchronizedQueue<T> {

        private final Queue<T> queue = new ArrayDeque<>();

        private void put(final T obj) {
            queue.add(obj);
            notify();
        }

        public synchronized void putAll(final List<T> list) {
            list.forEach(this::put);
        }

        public synchronized T take() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            return queue.poll();
        }

        public synchronized void forEach(final Consumer<T> consumer) {
            queue.forEach(consumer);
        }
    }

    private class Results<R> {

        private final List<R> results;
        private int resultsCountRest;
        private RuntimeException exception = null;

        private Results(final int size) {
            resultsCountRest = size;
            results = new ArrayList<>(Collections.nCopies(size, null));
        }

        synchronized void setResult(final int index, final R result) {
            results.set(index, result);
            incrementResultCounter();
        }

        synchronized void setException(final RuntimeException exception) {
            if (this.exception == null) {
                this.exception = exception;
            } else {
                this.exception.addSuppressed(exception);
            }
            incrementResultCounter();
        }

        private void incrementResultCounter() {
            resultsCountRest--; // :NOTE: -> 0
            if (resultsCountRest == 0) {
                notify();
            }
        }

        synchronized List<R> getResults() throws InterruptedException {
            while (resultsCountRest != 0 && !closed) {
                wait();
            }
            checkClosed();
            if (exception != null) {
                throw exception;
            }
            return results;
        }
    }
}
