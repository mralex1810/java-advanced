package info.kgeorgiy.ja.chulkov.arrayset;

import java.util.*;

public class AbstractArraySet<E> extends AbstractAbstractArraySet<E> {


    protected AbstractArraySet(List<E> array, Comparator<? super E> comparator) {
        super(array, comparator);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ReverseAbstractAbstractArraySet(new ReversedList<>(array), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ReversedList<>(array).iterator();
    }

    private class ReverseAbstractAbstractArraySet extends AbstractAbstractArraySet<E> {

        protected ReverseAbstractAbstractArraySet(List<E> array, Comparator<? super E> comparator) {
            super(array, comparator);
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return AbstractArraySet.this;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return AbstractArraySet.this.iterator();
        }
    }
}
