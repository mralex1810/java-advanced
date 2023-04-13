package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.concurrent.ListIP;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of {@link ListIP}
 */
public class IterativeParallelism implements AdvancedIP {

    // :NOTE: not even
    private static <T> List<List<T>> generateSubValuesList(final int threads, final List<T> values) {
        final int smallBucketSize = values.size() / threads;
        final int bigBucketSize = smallBucketSize + 1;
        final int bigBuckets = values.size() % threads;
        final int buckets = smallBucketSize == 0 ? bigBuckets : threads;
        int start = 0;
        final List<List<T>> subValuesList = new ArrayList<>(buckets);
        for (int i = 0; i < buckets; i++) {
            final int step = (i < bigBuckets ? bigBucketSize : smallBucketSize);
            subValuesList.add(values.subList(
                    start,
                    start + step
            ));
            start += step;
        }
        return subValuesList;

    }

    private static <T> void checkParameters(final int threads, final List<? extends T> values) {
        Objects.requireNonNull(values);
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads must be positive");
        }
    }

    protected static <T, R> R taskSchema(
            final int threads,
            final List<T> values,
            final Function<Stream<T>, R> threadTask,
            final Optional<Predicate<R>> terminateExecutionPredicate,
            final Function<Stream<R>, R> collectorFunction
    ) throws InterruptedException {
        checkParameters(threads, values);

        final List<List<T>> subValuesList = generateSubValuesList(threads, values);
        final List<OptionalNullable<R>> results =
                new ArrayList<>(Collections.nCopies(subValuesList.size(), OptionalNullable.empty()));

        final List<Thread> threadList = IntStream.range(0, threads)
                .mapToObj(it -> new Thread(
                        () -> results.set(it, OptionalNullable.of(threadTask.apply(subValuesList.get(it).stream())))
                )).toList();

        threadList.forEach(Thread::start);
        try {
            for (final Thread thread : threadList) {
                if (terminateExecutionPredicate.isPresent() &&
                        getObjectStreamForPresentedValues(results).anyMatch(terminateExecutionPredicate.get())) {
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

    private static <R> Stream<R> getObjectStreamForPresentedValues(final List<OptionalNullable<R>> results) {
        return results.stream()
                .filter(OptionalNullable::isPresent)
                .map(OptionalNullable::object);
    }

    protected static <T, R> R taskSchemaWithoutTerminating(
            final int threads,
            final List<T> values,
            final Function<Stream<T>, R> threadTask,
            final Function<Stream<R>, R> collectorFunction
    ) throws InterruptedException {
        return taskSchema(threads, values, threadTask, Optional.empty(), collectorFunction);
    }

    private static <T, R> List<R> streamOperationSchema(final int threads, final List<? extends T> values,
            final Function<Stream<? extends T>, Stream<? extends R>> operation) throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values,
                stream -> operation.apply(stream).collect(Collectors.toList()),
                stream -> stream.flatMap(List::stream).collect(Collectors.toList()));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException();
        }
        // :NOTE: .orElse(null)
        return taskSchemaWithoutTerminating(threads, values,
                stream -> stream.max(comparator).get(),
                stream -> stream.max(comparator).get());
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return taskSchema(threads, values,
                val -> val.allMatch(predicate),
                Optional.of(it -> !it),
                stream -> stream.allMatch(it -> it));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return taskSchema(threads, values,
                stream -> stream.anyMatch(predicate),
                Optional.of(it -> it),
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
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        // :NOTE: copy-paste
        return streamOperationSchema(threads, values, stream -> stream.filter(predicate));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values,
            final Function<? super T, ? extends U> f) throws InterruptedException {
        return streamOperationSchema(threads, values, stream -> stream.map(f));
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift,
            final Monoid<R> monoid) throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator())
        );
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
