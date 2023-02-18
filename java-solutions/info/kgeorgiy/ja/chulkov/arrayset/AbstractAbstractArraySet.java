package info.kgeorgiy.ja.chulkov.arrayset;

import java.util.*;

public abstract class AbstractAbstractArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    protected final List<E> array;
    protected final Comparator<? super E> comparator;

    protected AbstractAbstractArraySet(List<E> array, Comparator<? super E> comparator) {
        this.array = array;
        this.comparator = comparator;
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

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return subSet(safeSubList(fromElemWithInclusive(fromElement, fromInclusive),
                                    toElemWithInclusive(toElement, toInclusive)));
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return subSet(safeSubList(0, toElemWithInclusive(toElement, inclusive)));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return subSet(safeSubList(fromElemWithInclusive(fromElement, inclusive), size()));
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    private int lowerBound(E elem) {
        int res = rawBinarySearch(elem);
        return res >= 0 ? res : -res - 1;
    }

    private int upperBound(E elem) {
        int res = rawBinarySearch(elem);
        return res >= 0 ? res + 1 : -res - 1;
    }

    private int rawBinarySearch(E elem) {
        if (comparator == null) {
            return Collections.binarySearch((List<? extends Comparable<? super E>>) array, elem);
        } else {
            return Collections.binarySearch(array, elem, comparator);
        }
    }

    private List<E> safeSubList(int left, int right) {
        return left > right ? Collections.emptyList() : array.subList(left, right);
    }

    protected abstract NavigableSet<E> subSet(List<E> list);
//    {
//        return new AbstractArraySet<>(list, comparator());
//    }

    private int compare(E left, E rigth) {
        if (comparator == null) {
            return ((Comparable<? super E>) left).compareTo(rigth);
        } else {
            return comparator.compare(left, rigth);
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
    public boolean contains(Object o) {
        return rawBinarySearch((E) o) >= 0;
    }

}
