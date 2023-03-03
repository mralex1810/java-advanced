package info.kgeorgiy.ja.chulkov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    protected final List<E> array;
    protected final Comparator<? super E> comparator;
    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<? extends E> collection) {
        this(processSortedSetToList(new TreeSet<>(collection)), null);
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        this(processSortedSetToList(prepareSortedSet(collection, comparator)), comparator);
    }

    protected ArraySet(List<E> array, Comparator<? super E> comparator) {
        this.array = array;
        this.comparator = comparator;
    }

    private static <E> List<E> processSortedSetToList(SortedSet<E> set) {
        return Collections.unmodifiableList(new ArrayList<>(set));
    }

    private static <E> SortedSet<E> prepareSortedSet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        SortedSet<E> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);
        return treeSet;
    }
    private E validateIndexAndReturnElem(int index) {
        if (index >= 0 && index < size()) {
            return array.get(index);
        }
        return null;
    }

    @Override
    public E lower(E e) {
        return validateIndexAndReturnElem(lowerBound(e) - 1);
    }

    @Override
    public E floor(E e) {
        return validateIndexAndReturnElem(upperBound(e) - 1);
    }

    @Override
    public E ceiling(E e) {
        return validateIndexAndReturnElem(lowerBound(e));
    }

    @Override
    public E higher(E e) {
        return validateIndexAndReturnElem(upperBound(e));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        return array.iterator();
    }

    private int fromElemWithInclusive(E from, boolean inclusive) {
        return inclusive ? lowerBound(from) : upperBound(from);
    }

    private int toElemWithInclusive(E to, boolean inclusive) {
        return inclusive ? upperBound(to) : lowerBound(to);
    }

    private NavigableSet<E> safeSubSet(int left, int right) {
        return left > right ? Collections.emptyNavigableSet() : subSet(array.subList(left, right));
    }

    protected NavigableSet<E> subSet(List<E> list) {
        return new ArraySet<>(list, comparator());
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return safeSubSet(fromElemWithInclusive(fromElement, fromInclusive),
                toElemWithInclusive(toElement, toInclusive));
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return safeSubSet(0, toElemWithInclusive(toElement, inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return safeSubSet(fromElemWithInclusive(fromElement, inclusive), size());
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    private int uniBound(E elem, int resDelta) {
        int res = rawBinarySearch(elem);
        return res >= 0 ? res + resDelta : -res - 1;
    }

    private int lowerBound(E elem) {
        return uniBound(elem, 0);
    }

    private int upperBound(E elem) {
        return uniBound(elem, 1);
    }

    private int rawBinarySearch(E elem) {
        return Collections.binarySearch(array, elem, comparator);
    }

    @SuppressWarnings("unchecked")
    private int compare(E left, E right) {
        if (comparator == null) {
            return ((Comparable<? super E>) left).compareTo(right);
        } else {
            return comparator.compare(left, right);
        }
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        checkEmpty();
        return array.get(0);
    }

    @Override
    public E last() {
        checkEmpty();
        return array.get(array.size() - 1);
    }

    private void checkEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return rawBinarySearch((E) o) >= 0;
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ReversedArraySet(new ReversedListView<>(array), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }


    private class ReversedArraySet extends ArraySet<E> {

        protected ReversedArraySet(List<E> array, Comparator<? super E> comparator) {
            super(array, comparator);
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return ArraySet.this;
        }
    }
}
