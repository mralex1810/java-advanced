package info.kgeorgiy.ja.chulkov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
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
public class IterativeParallelism implements ListIP {

    private static <T> List<T> getSubValues(final int threads, final List<T> values, final int i) {
        if (i == threads - 1) {
            return values.subList(i * (values.size() / threads), values.size());
        }
        return values.subList(i * (values.size() / threads), (i + 1) * (values.size() / threads));
    }

    private static <T> void checkParameters(final int threads, final List<? extends T> values) {
        Objects.requireNonNull(values);
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads must be positive");
        }
    }

    protected static <T, R> R taskSchema(final int threads, final List<T> values,
            final Function<Stream<T>, R> threadTask,
            final Predicate<R> terminateExecutionPredicate,
            final Function<List<R>, R> collectorFunction) throws InterruptedException {
        checkParameters(threads, values);
        final List<OptionalNullable<R>> results =
                new ArrayList<>(Collections.nCopies(threads, OptionalNullable.empty()));
        final List<IndexedObject<Thread>> indexedThreadsList = IntStream.range(0, threads)
                .mapToObj(i -> new IndexedObject<>(i, getSubValues(threads, values, i)))
                .filter(it -> !it.value.isEmpty())
                .map(it -> new IndexedObject<>(it.index, new Thread(
                        () -> results.set(it.index, OptionalNullable.of(threadTask.apply(it.value.stream()))))))
                .toList();
        indexedThreadsList.forEach(it -> it.value.start());
        try {
            for (final IndexedObject<Thread> indexedThread : indexedThreadsList) {
                if (results.stream()
                                .anyMatch(it -> it.isPresent && terminateExecutionPredicate.test(it.object))) {
                    indexedThreadsList.forEach(it -> it.value.interrupt());
                    break;
                }
                indexedThread.value.join();
            }
        } catch (final InterruptedException e) {
            indexedThreadsList.forEach(it -> it.value.interrupt());
            throw e;
        }
        return collectorFunction.apply(
                results.stream().filter(OptionalNullable::isPresent).map(OptionalNullable::object).toList());
    }

    protected static <T, R> R taskSchemaWithoutTerminating(final int threads, final List<T> values,
            final Function<Stream<T>, R> threadTask,
            final Function<List<R>, R> collectorFunction) throws InterruptedException {
        return taskSchema(threads, values, threadTask, it -> false, collectorFunction);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException();
        }
        return taskSchemaWithoutTerminating(threads, values,
                stream -> stream.max(comparator).get(),
                list -> list.stream().max(comparator).orElse(null));
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
                it -> !it,
                list -> list.stream().allMatch(it -> it));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return taskSchema(threads, values,
                stream -> stream.anyMatch(predicate),
                it -> it,
                list -> list.stream().anyMatch(it -> it));
    }

    @Override
    public <T> int count(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values,
                stream -> (int) stream.filter(predicate).count(),
                list -> list.stream().mapToInt(it -> it).sum());
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values,
                stream -> stream.map(Objects::toString).collect(Collectors.joining()),
                list -> String.join("", list));
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                list -> list.stream().flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values,
            final Function<? super T, ? extends U> f) throws InterruptedException {
        return taskSchemaWithoutTerminating(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                list -> list.stream().flatMap(List::stream).collect(Collectors.toList()));
    }

    private record OptionalNullable<R>(R object, boolean isPresent) {

        public static <R> OptionalNullable<R> empty() {
            return new OptionalNullable<>(null, false);
        }

        public static <R> OptionalNullable<R> of(final R object) {
            return new OptionalNullable<>(object, true);
        }

    }

    private record IndexedObject<T>(int index, T value) {

    }

}
