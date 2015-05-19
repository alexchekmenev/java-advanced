package ru.ifmo.ctddev.chekmenev.iterativeparallelism;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by creed on 18.03.15.
 */

public class IterativeParallelism implements ListIP {
    private ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        mapper = null;
    }

    private class MyRunnable<T, R> implements Runnable {
        private List<? extends T> list;
        private Function<List<? extends T>, R> function;
        private R result;

        public MyRunnable(List<? extends T> list, Function<List<? extends T>, R> function) {
            this.list = list;
            this.function = function;
        }

        @Override
        public void run() {
            result = function.apply(list);
        }

        public R getResult() {
            return result;
        }
    }

    private <T, R> List<R> makeListWorkersWithoutMapper(int cnt, List<? extends T> list, Function<List<? extends T>, R> function) throws InterruptedException {
        Thread[] threads = new Thread[cnt];
        MyRunnable[] runnables = new MyRunnable[cnt];
        if (list.size() < cnt) {
            cnt = list.size();
        }
        int elementsPerThread = list.size() / cnt;
        for (int i = 0; i < cnt; i++) {
            int firstIndex = i * elementsPerThread;
            int lastIndex = firstIndex + elementsPerThread;
            if (i == cnt - 1 && lastIndex < list.size()) {
                lastIndex = list.size();
            }
            List<? extends T> subList = list.subList(firstIndex, lastIndex);
            runnables[i] = new MyRunnable<>(subList, function);
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }
        List<R> results = new ArrayList<>();
        for (int i = 0; i < cnt; i++) {
            threads[i].join();
            results.add((R) runnables[i].getResult());
        }
        return results;
    }

    private <T, R> List<R> makeListWorkers(int cnt, List<? extends T> list, Function<List<? extends T>, R> function) throws InterruptedException {
        if (mapper == null) {
            return makeListWorkersWithoutMapper(cnt, list, function);
        }
        if (list.size() < cnt) {
            cnt = list.size();
        }
        List<List<? extends T>> args = new ArrayList<>();
        int elementsPerThread = list.size() / cnt;
        for (int i = 0; i < cnt; i++) {
            int firstIndex = i * elementsPerThread;
            int lastIndex = firstIndex + elementsPerThread;
            if (i == cnt - 1 && lastIndex < list.size()) {
                lastIndex = list.size();
            }
            List<? extends T> subList = list.subList(firstIndex, lastIndex);
            args.add(subList);
        }
        return mapper.map(function, args);
    }

    /**
     * Transform {@code list} elements into one string (For example: {1, 2, -1} to "12-1")
     * <p>Function uses {@code cnt} threads for this
     *
     * @param cnt threads count
     * @param list list for transforming
     * @return representation of {@code list}
     * @throws InterruptedException if something gone wrong in some thread
     */
    @Override
    public String concat(int cnt, List<?> list) throws InterruptedException {
        StringBuilder ans = new StringBuilder();
        makeListWorkers(cnt, list, data -> {
            StringBuilder result = new StringBuilder();
            data.stream().map(Object::toString).forEach(result::append);
            return result.toString();
        }).forEach(ans::append);
        return ans.toString();
    }

    /**
     * Add to new list elements which returns true on {@code predicate}
     * <p>Function uses {@code cnt} threads for this
     *
     * @param cnt threads count
     * @param list list for filtering
     * @param predicate predicate for filtering
     * @param <T> type of elements
     * @return new filtered list
     * @throws InterruptedException if something gone wrong in some thread
     */
    @Override
    public <T> List<T> filter(int cnt, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        List<List<T>> localResults = makeListWorkers(cnt, list, new Function<List<? extends T>, List<T>>() {
            @Override
            public List<T> apply(List<? extends T> ts) {
                return ts.stream().filter(predicate).collect(Collectors.toList());
            }
        });

        List<T> result = new ArrayList<>();
        localResults.forEach(result::addAll);
        return result;
    }

    /**
     * Applies {@code function} to a all elements of list and adds this elements to new list
     * <p>Function uses {@code cnt} threads for this
     *
     * @param cnt threads count
     * @param list list for mapping
     * @param function function for mapping
     * @param <T> type of elements
     * @param <U> type of elements after applying
     * @return new mapped list
     * @throws InterruptedException if something gone wrong in some thread
     */
    @Override
    public <T, U> List<U> map(int cnt, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        List<List<U>> localResults = makeListWorkers(cnt, list, new Function<List<? extends T>, List<U>>() {
            @Override
            public List<U> apply(List<? extends T> ts) {
                return ts.stream().map(function).collect(Collectors.toList());
            }
        });

        List<U> result = new ArrayList<>();
        localResults.forEach(result::addAll);
        return result;
    }

    /**
     * Returns maximum of {@code list}'s elements using {@code comparator}
     * <p>Function uses {@code cnt} threads for this
     *
     * @param cnt threads count
     * @param list list for searching
     * @param comparator comparator of elements
     * @param <T> type of elements
     * @return maximum in {@code list}
     * @throws InterruptedException if something gone wrong in some thread
     */
    @Override
    public <T> T maximum(int cnt, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        Function function = new Function<List<? extends T>, T>() {
            @Override
            public T apply(List<? extends T> ts) {
                return ts.stream().max(comparator).get();
            }
        };
        List<T> localResults = makeListWorkers(cnt, list, function);
        return (T) function.apply(localResults);
    }

    /**
     * Returns minimum of {@code list}'s elements using {@code comparator}
     * <p>Function uses {@code cnt} threads for this
     *
     * @param cnt threads count
     * @param list list for searching
     * @param comparator comparator of elements
     * @param <T> type of elements
     * @return minimum in {@code list}
     * @throws InterruptedException if something gone wrong in some thread
     */
    @Override
    public <T> T minimum(int cnt, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        Function function = new Function<List<? extends T>, T>() {
            @Override
            public T apply(List<? extends T> ts) {
                return ts.stream().min(comparator).get();
            }
        };
        List<T> localResults = makeListWorkers(cnt, list, function);
        return (T) function.apply(localResults);
    }

    /**
     * Check that all elements of {@code list} returns true after applying to {@code predicate}
     * <p>Function uses {@code cnt} threads for this
     *
     * @param cnt threads count
     * @param list list for checking
     * @param predicate predicate for checking
     * @param <T> type of elements
     * @return {@code true} if all elements return true after applying and {@code false} otherwise
     * @throws InterruptedException if something gone wrong in some thread
     */
    @Override
    public <T> boolean all(int cnt, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        List<Boolean> localResults = makeListWorkers(cnt, list, new Function<List<? extends T>, Boolean>() {
            @Override
            public Boolean apply(List<? extends T> ts) {
                return ts.stream().allMatch(predicate);
            }
        });
        return localResults.stream().allMatch(new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                return aBoolean;
            }
        });
    }

    /**
     * Check that any element of {@code list} returns true after applying to {@code predicate}
     * <p>Function uses {@code cnt} threads for this
     *
     * @param cnt threads count
     * @param list list for checking
     * @param predicate predicate for checking
     * @param <T> type of elements
     * @return {@code true} if any element returns true after applying and {@code false} otherwise
     * @throws InterruptedException if something gone wrong in some thread
     */
    @Override
    public <T> boolean any(int cnt, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        List<Boolean> localResults = makeListWorkers(cnt, list, new Function<List<? extends T>, Boolean>() {
            @Override
            public Boolean apply(List<? extends T> ts) {
                return ts.stream().anyMatch(predicate);
            }
        });
        return localResults.stream().anyMatch(new Predicate<Boolean>() {
            @Override
            public boolean test(Boolean aBoolean) {
                return aBoolean;
            }
        });
    }
}