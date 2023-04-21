package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of {@link ListIP}
 */
public class IterativeParallelism implements AdvancedIP {

    private final TerminatingMapper mapper;

    /**
     * Constructs new {@link IterativeParallelism} by {@link ParallelMapper}. Uses {@link ParallelMapper} to run
     * parallel tasks.
     *
     * @param parallelMapper to run parallel map tasks.
     */
    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.mapper = new TerminatingMapper() {
            @Override
            public <T, R> List<R> baseMap(final Function<Stream<T>, R> threadTask,
                    final Predicate<R> terminateExecutionPredicate,
                    final List<Stream<T>> subValuesStreams) throws InterruptedException {
                return parallelMapper.map(threadTask, subValuesStreams);
            }
        };
    }

    /**
     * Creates default realisation of {@link IterativeParallelism}
     */
    public IterativeParallelism() {
        this.mapper = new IterativeParallelismBase();
    }

    static void checkThreads(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads must be positive");
        }
    }

    private static <T> Function<Stream<T>, T> maxSupport(final Comparator<? super T> comparator) {
        return stream -> stream.max(comparator).orElseThrow();
    }

    private static <T, R> Function<Stream<T>, R> streamMapReduceReduce(final Monoid<R> monoid, final Function<T, R> f) {
        return stream -> stream.map(f).reduce(monoid.getIdentity(), monoid.getOperator());
    }

    static <T> List<Stream<T>> generateSubValuesStreams(final int threads, final List<T> values) {
        checkArguments(threads, values);
        final int bucketSize = values.size() / threads;
        int rest = values.size() % threads;
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

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    static <T> void checkArguments(final int threads, final List<T> values) {
        IterativeParallelism.checkThreads(threads);
        Objects.requireNonNull(values);
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public <T> int count(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return mapReduce(threads, values, (it) -> predicate.test(it) ? 1 : 0, new Monoid<>(0, Integer::sum));
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return mapReduce(threads, values,
                Objects::toString,
                new Monoid<>("", String::concat)
        );
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values,
            final Predicate<? super T> predicate)
            throws InterruptedException {
        return flatStreamOperationSchema(threads, values, stream -> stream.filter(predicate));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values,
            final Function<? super T, ? extends U> f) throws InterruptedException {
        return flatStreamOperationSchema(threads, values, stream -> stream.map(f));
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values, maxSupport(comparator), maxSupport(comparator));
    }


    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid)
            throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return taskSchema(threads, values,
                val -> val.allMatch(predicate),
                it -> !it,
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    private  <T, R> List<R> flatStreamOperationSchema(
            final int threads, final List<? extends T> values,
            final Function<Stream<? extends T>, Stream<? extends R>> operation
    ) throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values,
                stream -> operation.apply(stream).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift,
            final Monoid<R> monoid) throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values,
                streamMapReduceReduce(monoid, lift),
                streamMapReduceReduce(monoid, Function.identity())
        );
    }

    private  <T, R> R taskSchema(
            final int threads,
            final List<T> values,
            final Function<Stream<T>, R> threadTask,
            final Predicate<R> terminateExecutionPredicate,
            final Function<Stream<R>, R> collectorFunction
    ) throws InterruptedException {
        return collectorFunction.apply(mapper.baseMap(
                threadTask,
                terminateExecutionPredicate,
                generateSubValuesStreams(threads, values)
        ).stream());
    }

    private  <T, R> R taskSchemaWithoutTerminating(
            final int threads,
            final List<T> values,
            final Function<Stream<T>, R> threadTask,
            final Function<Stream<R>, R> collectorFunction
    ) throws InterruptedException {
        return taskSchema(threads, values, threadTask, r -> false, collectorFunction);
    }
    @FunctionalInterface
    private interface TerminatingMapper {
        <T, R> List<R> baseMap(
                Function<Stream<T>, R> threadTask,
                Predicate<R> terminateExecutionPredicate,
                List<Stream<T>> subValuesStreams
        ) throws InterruptedException;
    }

    private static class IterativeParallelismBase implements TerminatingMapper {

        public  <T, R> List<R> baseMap(
                final Function<Stream<T>, R> threadTask,
                final Predicate<R> terminateExecutionPredicate,
                final List<Stream<T>> subValuesStreams
        ) throws InterruptedException {
            final VolatileBoolean terminated = new VolatileBoolean(false);
            final List<R> results = new ArrayList<>(Collections.nCopies(subValuesStreams.size(), null));
            final List<Thread> threadList = IntStream.range(0, subValuesStreams.size())
                    .mapToObj(it -> new Thread(() -> {
                        final var result = threadTask.apply(subValuesStreams.get(it));
                        results.set(it, result);
                        if (terminateExecutionPredicate.test(result)) {
                            terminated.set(true);
                        }
                    }
                    ))
                    .peek(Thread::start)
                    .toList();

            boolean reallyTerminated = false;

            InterruptedException exception = null;
            for (final Thread thread : threadList) {
                if (!reallyTerminated && terminated.isTrue()) {
                    if (terminated.isTrue()) {
                        threadList.forEach(Thread::interrupt);
                    }
                    reallyTerminated = true;
                }

                boolean joined = false;
                while (!joined) {
                    try {
                        thread.join();
                        joined = true;
                    } catch (final InterruptedException e) {
                        if (exception == null) {
                            if (!reallyTerminated) {
                                threadList.forEach(Thread::interrupt);
                            }
                            exception = e;
                        } else {
                            exception.addSuppressed(e);
                        }
                    }
                }
            }

            if (exception != null) {
                throw exception;
            }
            return results;
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
