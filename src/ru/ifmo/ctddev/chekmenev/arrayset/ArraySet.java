package ru.ifmo.ctddev.chekmenev.arrayset;


import com.sun.istack.internal.Nullable;

import java.util.*;

/**
 * Created by creed on 25.02.15.
 */

public class ArraySet<E> extends AbstractSet<E>
        implements NavigableSet<E>  {

    private List<E> set;
    private boolean notFailComparator = false;
    private Comparator<? super E> comparator;


    public ArraySet(Collection<? extends E> set, Comparator<? super E> comparator) {
        this.set = new ArrayList<>(set);
        this.comparator = comparator;

        if (this.comparator == null) {
            notFailComparator = true;
            this.comparator = new Comparator<E>() {
                @Override
                public int compare(E o1, E o2) {
                    if (o1 == null) {
                        return o2 == null ? 0 : -1;
                    }
                    if (o2 == null) {
                        return 1;
                    }
                    return ((Comparable)o1).compareTo(o2);
                }
            };
        }

        Collections.sort(this.set, comparator);
        List<E> unique = new ArrayList<>();
        for (E e : this.set) {
            if (unique.size() == 0 || this.comparator.compare(unique.get(unique.size() - 1), e) != 0) {
                unique.add(e);
            }
        }
        this.set = unique;
    }

    public ArraySet(Collection<? extends E> set) {
        this(set, null);
    }

    public ArraySet() {
        this(new ArrayList<E>(), null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(new ArrayList<E>(), comparator);
    }

    private ArraySet(List<E> set, Comparator<? super E> comparator) {
        this.set = set;
        this.comparator = comparator;
    }

    // -1 false : lower
    // -1 true : floor
    // 1 true : ceiling
    // 1 false : higher
    public int binSearchInd(E e, int signType, boolean canEqual) {
        boolean exists = true;
        int ind = Collections.binarySearch(set, e, comparator);
        if (ind < 0) {
            exists = false;
            ind = -ind - 1;
        }

        if (signType == -1) {
            if (canEqual) {
                if (!exists) {
                    ind--;
                }
            } else {
                ind--;
            }
        } else {
            if (!canEqual) {
                if (exists) {
                    ind++;
                }
            }
        }
        return ind;
    }

    @Override
    public E lower(E e) {
        int ind = binSearchInd(e, -1, false);
        return (ind >= 0 && ind < set.size() ? set.get(ind) : null);
    }

    @Override
    public E floor(E e) {
        int ind = binSearchInd(e, -1, true);
        return (ind >= 0 && ind < set.size() ? set.get(ind) : null);
    }

    @Override
    public E ceiling(E e) {
        int ind = binSearchInd(e, 1, true);
        return (ind >= 0 && ind < set.size() ? set.get(ind) : null);
    }

    @Override
    public E higher(E e) {
        int ind = binSearchInd(e, 1, false);
        return (ind >= 0 && ind < set.size() ? set.get(ind) : null);
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(set, (E)o, comparator) >= 0;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator<E> it = set.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return it.next();
            }
        };
    }

    @Override
    public NavigableSet<E> descendingSet() {
        List<E> descendingSet = new ReversedArrayList<>(set);
        return new ArraySet<>(descendingSet, comparator.reversed());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (size() == 0) {
            return new ArraySet<>(comparator);
        }

        int from = binSearchInd(fromElement, 1, fromInclusive);
        int to = binSearchInd(toElement, -1, toInclusive);

        if (from == -1 || to == -1 || from > to) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(set.subList(from, to + 1), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        if (size() == 0) {
            return new ArraySet<>(comparator);
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        if (size() == 0) {
            return new ArraySet<>(comparator);
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Nullable
    @Override
    public Comparator<? super E> comparator() {
        return notFailComparator ? null : comparator;
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
        if (set.size() == 0) {
            throw new NoSuchElementException();
        }
        return set.get(0);
    }

    @Override
    public E last() {
        if (set.size() == 0) {
            throw new NoSuchElementException();
        }
        return set.get(set.size() - 1);
    }

    // Unsupported operations
    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }
}
