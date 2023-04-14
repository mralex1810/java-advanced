package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of {@link ListIP}
 */
public class IterativeParallelism implements AdvancedIP {

    private final IterativeParallelismBase paralleler;

    /**
     * Constructs new {@link IterativeParallelism} by {@link ParallelMapper}. Uses {@link ParallelMapper} to run
     * parallel tasks.
     *
     * @param parallelMapper to run parallel map tasks.
     */
    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.paralleler = new IterativeParallelismBaseWithMapper(parallelMapper);
    }

    /**
     * Creates default realisation of {@link IterativeParallelism}
     */
    public IterativeParallelism() {
        this.paralleler = new IterativeParallelismBase();
    }

    static void checkThreads(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads must be positive");
        }
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        // :NOTE: .orElse(null)
        return paralleler.taskSchemaWithoutTerminating(threads, values,
                stream -> stream.max(comparator).orElseThrow(),
                // :NOTE: ??
                stream -> stream.max(comparator).orElseThrow());
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return paralleler.taskSchema(threads, values,
                val -> val.allMatch(predicate),
                it -> !it,
                stream -> stream.allMatch(it -> it));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return paralleler.taskSchema(threads, values,
                stream -> stream.anyMatch(predicate),
                it -> it,
                stream -> stream.anyMatch(it -> it));
    }

    @Override
    public <T> int count(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return mapReduce(threads, values, (it) -> predicate.test(it) ? 1 : 0, new Monoid<>(0, Integer::sum));
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return mapReduce(threads, values,
                Object::toString,
                new Monoid<>("", String::concat)
        );
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values,
            final Predicate<? super T> predicate)
            throws InterruptedException {
        // :NOTE: copy-paste
        return paralleler.flatStreamOperationSchema(threads, values, stream -> stream.filter(predicate));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values,
            final Function<? super T, ? extends U> f) throws InterruptedException {
        return paralleler.flatStreamOperationSchema(threads, values, stream -> stream.map(f));
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid)
            throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift,
            final Monoid<R> monoid) throws InterruptedException {
        return paralleler.taskSchemaWithoutTerminating(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator())
        );
    }

    static void joinThreadsWithSuppressAndTerminate(final BooleanSupplier toTerminate, final List<Thread> threadList) {
        RuntimeException exception = null;
        for (final Thread thread : threadList) {
            try {
                if (toTerminate.getAsBoolean()) {
                    threadList.forEach(Thread::interrupt);
                }
                thread.join();
            } catch (final InterruptedException e) {
                if (exception == null) {
                    exception = new RuntimeException("Tried to interrupt in close");
                }
                exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private static class IterativeParallelismBaseWithMapper extends IterativeParallelismBase {

        private final ParallelMapper parallelMapper;

        public IterativeParallelismBaseWithMapper(final ParallelMapper parallelMapper) {
            this.parallelMapper = parallelMapper;
        }

        @Override
        protected <T, R> Stream<R> baseMap(
                final Function<Stream<T>, R> threadTask,
                final Predicate<R> terminateExecutionPredicate,
                final List<Stream<T>> subValuesStreams
        ) throws InterruptedException {
            return parallelMapper.map(threadTask, subValuesStreams).stream();
        }
    }

    private static class IterativeParallelismBase {

        static <T> List<Stream<T>> generateSubValuesStreams(final int threads, final List<T> values) {
            IterativeParallelism.checkThreads(threads);
            Objects.requireNonNull(values);
            final int bucketSize = values.size() / threads;
            int rest = values.size() % threads;
            // :NOTE: simplify
            final int buckets = bucketSize == 0 ? rest : threads;
            int start = 0;
            final List<Stream<T>> subValuesList = new ArrayList<>(buckets);
            for (int i = 0; i < buckets; i++) {
                final int step = bucketSize + (--rest >= 0 ? 1 : 0);
                subValuesList.add(values.subList(start, start + step).stream());
                start += step;
            }
            return subValuesList;
        }

        protected <T, R> Stream<R> baseMap(
                final Function<Stream<T>, R> threadTask,
                final Predicate<R> terminateExecutionPredicate,
                final List<Stream<T>> subValuesStreams
        ) throws InterruptedException {
            final VolatileBoolean terminate = new VolatileBoolean(false);
            final List<R> results =
                    new ArrayList<>(Collections.nCopies(subValuesStreams.size(), null));
            final List<Thread> threadList = IntStream.range(0, subValuesStreams.size())
                    .mapToObj(it -> new Thread(() -> {
                        final var result = threadTask.apply(subValuesStreams.get(it));
                        // :NOTE: action order
                        results.set(it, result);
                        if (terminateExecutionPredicate != null && terminateExecutionPredicate.test(result)) {
                            terminate.set(true);
                        }
                    }
                    )).toList();
            threadList.forEach(Thread::start);
            joinThreadsWithSuppressAndTerminate(terminate::isTrue, threadList);  // :NOTE: join
            return results.stream();
        }

        protected <T, R> R taskSchema(
                final int threads,
                final List<T> values,
                final Function<Stream<T>, R> threadTask,
                final Predicate<R> terminateExecutionPredicate,
                final Function<Stream<R>, R> collectorFunction
        ) throws InterruptedException {
            return collectorFunction.apply(baseMap(
                    threadTask,
                    terminateExecutionPredicate,
                    generateSubValuesStreams(threads, values)
            ));
        }

        protected <T, R> R taskSchemaWithoutTerminating(
                final int threads,
                final List<T> values,
                final Function<Stream<T>, R> threadTask,
                final Function<Stream<R>, R> collectorFunction
        ) throws InterruptedException {
            return taskSchema(threads, values, threadTask, null, collectorFunction);
        }

        protected <T, R> List<R> flatStreamOperationSchema(final int threads, final List<? extends T> values,
                final Function<Stream<? extends T>, Stream<? extends R>> operation) throws InterruptedException {
            return taskSchemaWithoutTerminating(threads, values,
                    stream -> operation.apply(stream).collect(Collectors.toList()),
                    stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
        }
    }

    private static class VolatileBoolean {

        private volatile boolean bool;

        public VolatileBoolean(final boolean bool) {
            this.bool = bool;
        }

        public boolean isTrue() {
            return bool;
        }

        public void set(final boolean bool) {
            this.bool = bool;
        }
    }
}
