package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
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

    private final IterativeParallelismBase paralleler;

    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.paralleler = new IterativeParallelismBaseWithMapper(parallelMapper);
    }

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
                stream -> stream.max(comparator).orElseThrow(NoSuchElementException::new));
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

    private static class IterativeParallelismBaseWithMapper extends IterativeParallelismBase {

        private final ParallelMapper parallelMapper;

        public IterativeParallelismBaseWithMapper(final ParallelMapper parallelMapper) {
            this.parallelMapper = parallelMapper;
        }

        @Override
        protected <T, R> R taskSchema(
                final int threads,
                final List<T> values,
                final Function<Stream<T>, R> threadTask,
                final Predicate<R> terminateExecutionPredicate,
                final Function<Stream<R>, R> collectorFunction
        ) throws InterruptedException {
            return collectorFunction.apply(
                    parallelMapper.map(threadTask, generateSubValuesStreams(threads, values)).stream());
        }
    }

    private static class IterativeParallelismBase {


        // :NOTE: not even
        static <T> List<Stream<T>> generateSubValuesStreams(final int threads, final List<T> values) {
            IterativeParallelism.checkThreads(threads);
            Objects.requireNonNull(values);
            final int smallBucketSize = values.size() / threads;
            final int bigBucketSize = smallBucketSize + 1;
            final int bigBuckets = values.size() % threads;
            final int buckets = smallBucketSize == 0 ? bigBuckets : threads;
            int start = 0;
            final List<Stream<T>> subValuesList = new ArrayList<>(buckets);
            for (int i = 0; i < buckets; i++) {
                final int step = (i < bigBuckets ? bigBucketSize : smallBucketSize);
                subValuesList.add(values.subList(
                        start,
                        start + step
                ).stream());
                start += step;
            }
            return subValuesList;

        }

        private static <R> Stream<R> getObjectStreamForPresentedValues(final List<OptionalNullable<R>> results) {
            return results.stream()
                    .filter(OptionalNullable::isPresent)
                    .map(OptionalNullable::object);
        }

        private static <R> boolean checkTerminate(final Predicate<R> terminateExecutionPredicate,
                final List<OptionalNullable<R>> results) {
            return terminateExecutionPredicate != null &&
                    getObjectStreamForPresentedValues(results).anyMatch(
                            terminateExecutionPredicate);
        }

        protected <T, R> R taskSchema(
                final int threads,
                final List<T> values,
                final Function<Stream<T>, R> threadTask,
                final Predicate<R> terminateExecutionPredicate,
                final Function<Stream<R>, R> collectorFunction
        ) throws InterruptedException {
            final List<Stream<T>> subValuesStreams = generateSubValuesStreams(threads, values);
            final List<OptionalNullable<R>> results =
                    new ArrayList<>(Collections.nCopies(subValuesStreams.size(), OptionalNullable.empty()));
            final List<Thread> threadList = IntStream.range(0, threads)
                    .mapToObj(it -> new Thread(
                            () -> results.set(it, OptionalNullable.of(threadTask.apply(subValuesStreams.get(it))))
                    )).toList();
            threadList.forEach(Thread::start);
            try {
                for (final Thread thread : threadList) {
                    if (checkTerminate(terminateExecutionPredicate, results)) {
                        threadList.forEach(Thread::interrupt);
                        break;
                    }
                    thread.join();
                }
            } catch (final InterruptedException e) {
                threadList.forEach(Thread::interrupt);
                throw e;
            }

            return collectorFunction.apply(getObjectStreamForPresentedValues(results));
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

        private record OptionalNullable<R>(R object, boolean isPresent) {

            private static final OptionalNullable<?> EMPTY = new OptionalNullable<>(null, false);

            @SuppressWarnings("unchecked")
            public static <R> OptionalNullable<R> empty() {
                return (OptionalNullable<R>) EMPTY;
            }

            public static <R> OptionalNullable<R> of(final R object) {
                return new OptionalNullable<>(object, true);
            }

        }
    }

    private static class IterativeParallelismBaseWithoutTerminating extends IterativeParallelismBase {

        @Override
        protected <T, R> R taskSchema(
                final int threads,
                final List<T> values,
                final Function<Stream<T>, R> threadTask,
                final Predicate<R> terminateExecutionPredicate, final
        Function<Stream<R>, R> collectorFunction)
                throws InterruptedException {
            final List<Stream<T>> subValuesStreams = generateSubValuesStreams(threads, values);
            final List<R> results =
                    new ArrayList<>(Collections.nCopies(subValuesStreams.size(), null));
            final List<Thread> threadList = IntStream.range(0, threads)
                    .mapToObj(it -> new Thread(
                            () -> results.set(it, threadTask.apply(subValuesStreams.get(it)))
                    )).toList();
            threadList.forEach(Thread::start);
            try {
                for (final Thread thread : threadList) {
                    thread.join();
                }
            } catch (final InterruptedException e) {
                threadList.forEach(Thread::interrupt);
                throw e;
            }

            return collectorFunction.apply(results.stream());
        }
    }

}
