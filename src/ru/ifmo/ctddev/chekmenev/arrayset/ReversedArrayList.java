package ru.ifmo.ctddev.chekmenev.arrayset;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by creed on 05.03.15.
 */

public class ReversedArrayList<E> extends AbstractList<E> {
    private List<E> forwardList;

    public ReversedArrayList(List<E> forwardList) {
        this.forwardList = forwardList;
    }

    @Override
    public int size() {
        return forwardList.size();
    }

    @Override
    public E get(int index) {
        return forwardList.get(size() - index - 1);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int it = 0;

            @Override
            public boolean hasNext() {
                return it != size();
            }

            @Override
            public E next() {
                if (it >= size()) {
                    throw new NoSuchElementException();
                }
                return get(it++);
            }
        };
    }
}
