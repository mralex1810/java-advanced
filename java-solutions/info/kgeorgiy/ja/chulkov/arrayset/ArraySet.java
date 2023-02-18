package info.kgeorgiy.ja.chulkov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractArraySet<E> {

    public ArraySet() {
        super(Collections.emptyList(), null);
    }

    public ArraySet(Collection<? extends E> collection) {
        super(processSortedSetToList(new TreeSet<>(collection)), null);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        super(processSortedSetToList(prepareSortedSet(collection, comparator)), comparator);
    }

    private static <E> List<E> processSortedSetToList(SortedSet<E> set) {
        return Collections.unmodifiableList(new ArrayList<>(set));
    }

    private static <E> SortedSet<E> prepareSortedSet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        SortedSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        return treeSet;
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ReverseAbstractArraySet(new ReversedList<>(array), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ReversedList<>(array).iterator();
    }

    private class ReverseAbstractArraySet extends AbstractArraySet<E> {

        protected ReverseAbstractArraySet(List<E> array, Comparator<? super E> comparator) {
            super(array, comparator);
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return ArraySet.this;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return ArraySet.this.iterator();
        }
    }
}
