package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

/**
 * Implementation of {@link ParallelMapper}
 */
public class ParallelMapperImpl implements ParallelMapper {

    private static final int MAX_QUEUE_SIZE = 1 << 20;
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

    private synchronized void addTask(final Runnable task) throws InterruptedException {
        while (tasksQueue.size() > MAX_QUEUE_SIZE) {
            tasksQueue.wait();
        }
        synchronized (tasksQueue) {
            tasksQueue.add(task);
        }
        notify();
    }

    private synchronized Runnable getTask() throws InterruptedException {
        while (tasksQueue.isEmpty()) {
            wait();
        }
        synchronized (tasksQueue) {
            final var res = tasksQueue.poll();
            tasksQueue.notify();
            return res;
        }
    }

    public <T, R> List<R> map(
            final Function<? super T, ? extends R> f,
            final List<? extends T> args
    ) throws InterruptedException {
        final Results<R> results = new Results<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int finalI = i;
            addTask(() -> results.setResult(finalI, f.apply(args.get(finalI))));
        }
        return results.getResults();
    }

    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        try {
            for (final Thread thread : threads) {
                thread.join();
            }
        } catch (final InterruptedException e) {
            System.err.println("Thread was interrupted on closing: " + e.getMessage());
        }
    }

    private static class Results<R> {

        private final List<R> results;
        private int resultsCount;

        private Results(final int size) {
            results = new ArrayList<>(Collections.nCopies(size, null));
        }

        void setResult(final int index, final R result) {
            results.set(index, result);
            synchronized (this) {
                resultsCount++;
                if (resultsCount == results.size()) {
                    this.notify();
                }
            }
        }

        synchronized List<R> getResults() throws InterruptedException {
            while (resultsCount < results.size()) {
                this.wait();
            }
            return results;
        }
    }
}