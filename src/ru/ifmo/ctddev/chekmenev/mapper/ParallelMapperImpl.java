package ru.ifmo.ctddev.chekmenev.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by creed on 31.03.15.
 */

public class ParallelMapperImpl implements ParallelMapper {
    private int threadCount;
    private LinkedList<Runnable> queue;
    private Thread[] threads;

    /**
     * Creates {@code threadCount} worker threads for parallel work
     *
     * @param threadCount count of worker thread
     */
    public ParallelMapperImpl(int threadCount) {
        this.threadCount = threadCount;
        threads = new Thread[threadCount];
        queue = new LinkedList<>();
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                Runnable r;
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (queue) {
                        while (queue.isEmpty()) {
                            try {
                                queue.wait();
                            } catch (InterruptedException e) {
                                return;
                            }
                        }
                        r = queue.removeFirst();
                    }
                    r.run();
                }
            });
            threads[i].start();
        }
    }

    /**
     * Get list and function and returns list of results of function applying to every element of the list
     *
     * @param function function for applying
     * @param list list of arguments to apply
     * @param <T> type of the function argument and the list elements
     * @param <R> type of the function returning result
     * @return list of applying results to every element of {@code list}
     * @throws InterruptedException if any thread interrupted the current thread before or while the current thread was working
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        MyRunnable[] runnables = new MyRunnable[list.size()];
        for (int i = 0; i < list.size(); i++) {
            MyRunnable r = new MyRunnable(list.get(i), function);
            runnables[i] = r;
            synchronized (queue) {
                queue.addLast(r);
                queue.notifyAll();
            }
        }
        List<R> results = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            synchronized (runnables[i]) {
                while (runnables[i].getResult() == null) {
                    runnables[i].wait();
                }
                results.add((R) runnables[i].getResult());
            }
        }
        return results;
    }

    /**
     * Stops all worker threads
     *
     * @throws InterruptedException if any thread interrupted the current thread before or while the current thread was working
     */
    @Override
    public void close() throws InterruptedException {
        for (int i = 0; i < threadCount; i++) {
            threads[i].interrupt();
        }
    }

    private class MyRunnable<T, R> implements Runnable {
        private T argument;
        private Function<? super T, ? extends R> function;
        private R result;

        public MyRunnable(T argument, Function<? super T, ? extends R> function) {
            this.argument = argument;
            this.function = function;
        }

        @Override
        public void run() {
            result = function.apply(argument);
            synchronized (this) {
                notifyAll();
            }
        }

        public R getResult() {
            return result;
        }
    }
}