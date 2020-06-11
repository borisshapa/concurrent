package ru.ifmo.rain.shaposhnikov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * List iterative parallelism support.
 *
 * @author Boris Shaposhnikov
 */
public class IterativeParallelism implements ListIP, AdvancedIP {

    private final MapReduce mapReduce;

    public IterativeParallelism(final ParallelMapper parallelMapper) {
        mapReduce = new MapReduce(parallelMapper);
    }

    public IterativeParallelism() {
        mapReduce = new MapReduce();
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return mapReduce.work(threads, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    private <T> List<T> collectToList(final Stream<? extends List<? extends T>> streams) {
        return streams.flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return mapReduce.work(threads, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                this::collectToList);
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return mapReduce.work(threads, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                this::collectToList);
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Expected non empty list");
        }
        final Function<Stream<? extends T>, T> max = stream -> stream.max(comparator).orElse(null);
        return mapReduce.work(threads, values, max, max);
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return mapReduce.work(threads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, Predicate.not(predicate));
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), monoid);
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        return mapReduce.work(threads, values,
                stream -> stream.map(lift).reduce(monoid.getIdentity(), monoid.getOperator()),
                stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator(), monoid.getOperator()));
    }
}
