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
    private final SynchronizedQueue tasks = new SynchronizedQueue();

    private final List<Thread> threads;

    /**
     * Constructs new {@code ParallelMapperImpl} by number of workers threads.
     *
     * @param threadsNum number of thread workers in constructing {@code ParallelMapperImpl}
     */
    public ParallelMapperImpl(final int threadsNum) {
        IterativeParallelism.checkThreads(threadsNum);
        this.threads = new ArrayList<>(threadsNum);
        for (int i = 0; i < threadsNum; i++) {
            threads.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        tasks.getTask().run();
                    }
                } catch (final InterruptedException ignored) {
                }
            }));
            threads.get(i).start();
        }
    }

    public <T, R> List<R> map(
            final Function<? super T, ? extends R> f,
            final List<? extends T> args
    ) throws InterruptedException {
        final Results<R> results = new Results<>(args.size());
        synchronized (tasks) {
            IntStream.range(0, args.size())
                    .forEach(index -> tasks.addTask(() -> {
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

    private static class SynchronizedQueue {

        private final Queue<Runnable> tasksQueue = new ArrayDeque<>();

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

    }

    private static class Results<R> {

        private final List<R> results;
        private int resultsCount;
        private RuntimeException exception = null;

        private Results(final int size) {
            results = new ArrayList<>(Collections.nCopies(size, null));
        }

        synchronized void setResult(final int index, final R result) {
            results.set(index, result);
            incrementResultCounter();
        }

        synchronized List<R> getResults() throws InterruptedException {
            while (resultsCount < results.size()) {
                wait();
            }
            if (exception != null) {
                throw exception;
            }
            return results;
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
            resultsCount++;
            if (resultsCount == results.size()) {
                notify();
            }
        }
    }
}
