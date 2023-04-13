package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {

    private final ParallelMapper parallelMapper;

    public ParallelMapperImpl(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    public ParallelMapperImpl(final int threadsNum) {
        this.parallelMapper = new ParallelMapperImplInner(threadsNum);
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args)
            throws InterruptedException {
        return parallelMapper.map(f, args);
    }

    @Override
    public void close() {
        parallelMapper.close();
    }


    private static class ParallelMapperImplInner implements ParallelMapper {


        final Queue<Runnable> tasksQueue;
        final List<Thread> threads;

        public ParallelMapperImplInner(final int threadsNum) {
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

        private synchronized void addTask(final Runnable task) {
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
                return tasksQueue.poll();
            }
        }

        public <T, R> List<R> map(
                final Function<? super T, ? extends R> f,
                final List<? extends T> args
        ) throws InterruptedException {
            final Results<R> results = new Results<>(args.size());
            IntStream.range(0, args.size())
                    .mapToObj(index -> (Runnable) () -> results.setResult(index, f.apply(args.get(index))))
                    .forEach(this::addTask);
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

}
