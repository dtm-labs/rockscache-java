package io.github.dtm.cache.java;

@FunctionalInterface
public interface Loader<T> {

    T load(String key);
}
