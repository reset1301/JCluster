package ru.rrr.function;

@FunctionalInterface
public interface UncheckedFunction<T, R> {
    R apply(T t) throws Exception;
}
