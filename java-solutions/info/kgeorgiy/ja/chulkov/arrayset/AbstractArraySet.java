package info.kgeorgiy.ja.chulkov.arrayset;

import java.util.*;

public class AbstractArraySet<E> extends AbstractAbstractArraySet<E> {

    protected AbstractArraySet(List<E> array, Comparator<? super E> comparator) {
        super(array, comparator);
    }

    @Override
    protected NavigableSet<E> subSet(List<E> list) {
        return new AbstractArraySet<>(list, comparator());
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ReversedAbstractArraySet(new ReversedList<>(array), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new ReversedList<>(array).iterator();
    }



    private class ReversedAbstractArraySet extends AbstractAbstractArraySet<E> {

        protected ReversedAbstractArraySet(List<E> array, Comparator<? super E> comparator) {
            super(array, comparator);
        }

        @Override
        protected NavigableSet<E> subSet(List<E> list) {
            return new ReversedAbstractArraySet(list, comparator());
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
