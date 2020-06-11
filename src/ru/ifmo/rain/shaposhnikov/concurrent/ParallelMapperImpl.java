package ru.ifmo.rain.shaposhnikov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Performing tasks on the specified number of threads.
 *
 * @author Boris Shaposhnikov
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final List<Thread> threads;
    private final SynchronizedQueue tasks;
    private volatile boolean closed;
    private int running;

    private static void launchThread(final List<Thread> threads, final Runnable threadWork) {
        final Thread thread = new Thread(threadWork);
        threads.add(thread);
        thread.start();
    }

    /**
     * Creates the specified number of threads.
     *
     * @param threads how many threads are needed.
     */
    public ParallelMapperImpl(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Expected a positive number of threads");
        }

        this.threads = new ArrayList<>(threads);
        this.tasks = new SynchronizedQueue();

        for (int i = 0; i < threads; i++) {
            launchThread(this.threads, () -> {
                try {
                    while (!Thread.interrupted()) {
                        tasks.poll().run();
                    }
                } catch (final InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    private synchronized void increaseNumberOfRunning() {
        running++;
    }

    private synchronized void decreaseNumberOfRunning() {
        if (--running == 0) {
            notify();
        }
    }

    private synchronized void waitAllMapCalls() {
        while (running > 0) {
            try {
                wait();
            } catch (final InterruptedException ignored) {
            }
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final ResultList<R> resultList = new ResultList<>(args.size());
        if (!closed) {
            increaseNumberOfRunning();
            for (int i = 0; i < args.size(); i++) {
                final int index = i;
                tasks.add(() -> {
                    R funValue = null;
                    try {
                        if (!closed) {
                            funValue = f.apply(args.get(index));
                        }
                    } catch (final RuntimeException e) {
                        resultList.setException(e);
                    }
                    resultList.set(index, funValue);
                });
            }
            decreaseNumberOfRunning();
        }
        return resultList.getList();
    }

    @Override
    public void close() {
        closed = true;
        threads.forEach(Thread::interrupt);
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (final InterruptedException ignored) {
                i--;
            }
        }
        waitAllMapCalls();
        tasks.getRunning().forEach(Runnable::run);
    }


    private static class SynchronizedQueue {

        private final static int UPPER_BOUND = Integer.MAX_VALUE;

        private final Queue<Runnable> tasks;

        public SynchronizedQueue() {
            tasks = new ArrayDeque<>();
        }

        public synchronized int size() {
            return tasks.size();
        }

        public synchronized void add(final Runnable task) throws InterruptedException {
            while (tasks.size() == UPPER_BOUND) {
                wait();
            }
            tasks.add(task);
            notify();
        }

        public synchronized boolean isEmpty() {
            return tasks.isEmpty();
        }

        public synchronized Runnable poll() throws InterruptedException {
            while (tasks.isEmpty()) {
                wait();
            }
            return tasks.poll();
        }

        public synchronized Queue<Runnable> getRunning() {
            return tasks;
        }
    }

    private static class ResultList<T> {
        private final List<T> list;
        private int done = 0;
        private RuntimeException exception = null;

        ResultList(final int capacity) {
            list = new ArrayList<>(Collections.nCopies(capacity, null));
        }

        public synchronized void set(final int index, final T element) {
            list.set(index, element);
            if (++done == list.size()) {
                notify();
            }
        }

        public synchronized void setException(final RuntimeException e) {
            exception = e;
        }

        public synchronized List<T> getList() throws InterruptedException {
            if (exception != null) {
                throw exception;
            }

            while (done < list.size()) {
                wait();
            }
            return list;
        }
    }
}
