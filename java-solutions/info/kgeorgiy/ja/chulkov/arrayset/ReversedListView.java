package info.kgeorgiy.ja.chulkov.arrayset;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;

public class ReversedListView<E> extends AbstractList<E> implements List<E> {

    private final List<E> array;

    public ReversedListView(List<E> array) {
        this.array = array;
    }

    @Override
    public E get(int index) {
        Objects.checkIndex(index, size());
        return array.get(size() - index - 1);
    }

    @Override
    public int size() {
        return array.size();
    }
}
