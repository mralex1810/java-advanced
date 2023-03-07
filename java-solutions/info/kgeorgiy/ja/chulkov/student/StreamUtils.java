package info.kgeorgiy.ja.chulkov.student;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamUtils {

    public static <T, R, CR, A> CR processCollectionByStream(Collection<T> input, Function<Stream<T>, Stream<R>> map,
                                                             Collector<R, A, CR> collector) {
        return map.apply(input.stream())
                .collect(collector);
    }

    public static <T, A, R, CR> CR mapCollection(Collection<T> input, Function<T, R> function, Collector<R, A, CR> collector) {
        return processCollectionByStream(input, stream -> stream.map(function), collector);
    }

    public static <T, R> List<R> mapCollectiontoList(Collection<T> input, Function<T, R> function) {
        return mapCollection(input, function, Collectors.toList());
    }

    public static <T, R> R maxAndMapOptional(Collection<T> input, Comparator<T> comparator,
                                             Function<T, R> mapper, R defaultValue) {
        return input.stream()
                .max(comparator)
                .map(mapper)
                .orElse(defaultValue);
    }

    public static <T, A, CR> CR sortCollectionByComparator(Collection<T> input, Comparator<T> comparator,
                                                           Collector<T, A, CR> collector) {
        return processCollectionByStream(input, stream -> stream.sorted(comparator), collector);
    }

    public static <T> List<T> sortCollectionByComparatorToList(Collection<T> input, Comparator<T> comparator) {
        return sortCollectionByComparator(input, comparator, Collectors.toList());
    }

    public static  <T, R> List<R> mapByIndicesOnList(List<T> collection, int[] indices, Function<T, R> mapper) {
        return Arrays.stream(indices).mapToObj(collection::get).map(mapper).toList();
    }

    public static <T, R> List<R> mapByIndices(Collection<T> collection, int[] indices, Function<T, R> mapper) {
        return mapByIndicesOnList(List.copyOf(collection), indices, mapper);
    }

}
