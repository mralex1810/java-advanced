package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Implementation of {@link ParallelMapper}
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final Queue<Runnable> tasksQueue;
    private final List<Thread> threads;

    /**
     * Constructs new {@code ParallelMapperImpl} by number of workers threads.
     *
     * @param threadsNum number of thread workers in constructing {@code ParallelMapperImpl}
     */
    public ParallelMapperImpl(final int threadsNum) {
        IterativeParallelism.checkThreads(threadsNum);
        tasksQueue = new ArrayDeque<>();
        this.threads = new ArrayList<>(threadsNum);
        for (int i = 0; i < threadsNum; i++) {
            threads.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        getTask().run();
                    }
                } catch (final InterruptedException ignored) {
                }
            }));
            threads.get(i).start();
        }
    }

    private void addTask(final Runnable task) {
        tasksQueue.add(task);
        notify();
    }

    // :NOTE: double synchronization
    private synchronized Runnable getTask() throws InterruptedException {
        while (tasksQueue.isEmpty()) {
            wait();
        }
        return tasksQueue.poll();
    }

    public <T, R> List<R> map(
            final Function<? super T, ? extends R> f,
            final List<? extends T> args
    ) throws InterruptedException {
        final Results<R> results = new Results<>(args.size());
        synchronized (this) {
            IntStream.range(0, args.size())
                    .forEach(index -> addTask(() -> {
                        try {
                            results.setResult(index, f.apply(args.get(index)));
                        } catch (final RuntimeException e) {
                            results.setException(new RuntimeException("Error on mapping", e));
                        }
                    }));
        }
        return results.getResults();
    }

    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        RuntimeException exception = null;
        for (final Thread thread : threads) {
            try {
                thread.join();
            } catch (final InterruptedException e) {
                if (exception == null) { // :NOTE: join
                    exception = new RuntimeException("Interrupt on closing");
                }
                exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            throw exception;
        }

    }

    private static class Results<R> {

        private final List<R> results;
        private int resultsCount;
        private RuntimeException exception = null;

        private Results(final int size) {
            results = new ArrayList<>(Collections.nCopies(size, null));
        }

        void setResult(final int index, final R result) {
            results.set(index, result);
            synchronized (this) {
                resultsCount++;
                if (resultsCount == results.size()) {
                    notify();
                }
            }
        }

        synchronized List<R> getResults() throws InterruptedException {
            while (resultsCount < results.size() && exception == null) {
                wait();
            }
            if (exception != null) {
                throw exception;
            }
            return results;
        }

        synchronized void setException(final RuntimeException exception) {
            this.exception = exception;
            notify();
        }
    }
}
