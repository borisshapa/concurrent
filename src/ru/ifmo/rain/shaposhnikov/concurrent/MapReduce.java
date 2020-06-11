package ru.ifmo.rain.shaposhnikov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The implementation of the MapReduce model.
 * Static class for parallel execution of independent tasks on pieces of data and combining results.
 *
 * @author Boris Shaposhnikov
 */
public class MapReduce {

    private final ParallelMapper parallelMapper;

    public MapReduce(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    public MapReduce() {
        this.parallelMapper = null;
    }

    private static class Task<T, R> {
        private R result;
        private final Thread thread;

        public Task(final Function<Stream<? extends T>, ? extends R> map, final Stream<? extends T> block) {
            this.thread = new Thread(() -> result = map.apply(block));
            this.thread.start();
        }

        public R getResult() throws InterruptedException {
            thread.join();
            return result;
        }

        public void interrupt() {
            thread.interrupt();
        }
    }


    public <M, R> R reduce(final List<M> data, final Function<Stream<? extends M>, R> reducer) {
        return reducer.apply(data.stream());
    }


    public <T, R> List<R> map(final Function<Stream<? extends T>, ? extends R> map, final List<Stream<? extends T>> blocks) throws InterruptedException {
        final List<Task<T, R>> threads = new ArrayList<>();
        for (final Stream<? extends T> block : blocks) {
            threads.add(new Task<>(map, block));
        }
        final List<R> result = new ArrayList<>();
        InterruptedException interruptedException = null;
        for (int i = 0; i < threads.size(); i++) {
            try {
                result.add(threads.get(i).getResult());
            } catch (final InterruptedException e) {
                interruptedException = e;
                for (int j = i; j < threads.size(); j++) {
                    threads.get(j).interrupt();
                }
                i--;
            }
        }
        if (interruptedException != null) {
            throw interruptedException;
        }
        return result;
    }

    private static <T> List<Stream<? extends T>> split(final int numberOfBlocks, final List<? extends T> data) throws IllegalArgumentException {
        if (numberOfBlocks <= 0) {
            throw new IllegalArgumentException("A positive number of threads was expected");
        }

        final int blockSize = data.size() / numberOfBlocks;
        int remainder = data.size() % numberOfBlocks;

        final List<Stream<? extends T>> blocks = new ArrayList<>();
        int lowerBound = 0;
        while (lowerBound < data.size()) {
            final int upper_bound = lowerBound + blockSize + (remainder-- > 0 ? 1 : 0);
            blocks.add(data.subList(lowerBound, Math.min(upper_bound, data.size())).stream());
            lowerBound = upper_bound;
        }
        return blocks;
    }

    /**
     * Starts the MapReduce execution. List of <var>values</var> is distributed on the specified number of blocks,
     * approximately the same size. Independently for each block, a function <var>map</var>
     * in a separate thread is applied. The results for each block are combined by a function <var>reduce</var>.
     *
     * @param threads number of threads to run in parallel
     * @param values  the list of elements for which it is necessary to calculate the given function
     * @param map     function from {@link Stream} to type <var>M</var>
     * @param reduce  the function of combining the results of the work of threads, from {@link Stream}
     *                of items that have <var>M</var> type to type <var>R</var>
     * @param <T>     elements of this type are expected in the list
     * @param <M>     intermediate data type between operations Map and Reduce
     * @param <R>     expected result type
     * @return The result of applying the function <var>map</var> to the list <var>values</var>
     * @throws InterruptedException     if any thread has interrupted the current thread
     * @throws IllegalArgumentException if <var>threads</var> isn't a positive number
     */
    public <T, M, R> R work(final int threads,
                            final List<? extends T> values,
                            final Function<Stream<? extends T>, ? extends M> map,
                            final Function<Stream<? extends M>, R> reduce) throws InterruptedException {

        final int numberOfThreads = Math.min(threads, values.size());

        final List<Stream<? extends T>> splittedData = split(numberOfThreads, values);

        final List<M> mapList = parallelMapper != null
                ? parallelMapper.map(map, splittedData)
                : map(map, splittedData);
        return reduce.apply(mapList.stream());
    }
}