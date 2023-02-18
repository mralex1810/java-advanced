package info.kgeorgiy.ja.chulkov.arrayset;

import java.util.*;

public class ReversedList<E> extends AbstractList<E>  implements List<E> {

    private final List<E> array;

    public ReversedList(List<E> array) {
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
