package io.github.dtm.cache.java;

@FunctionalInterface
public interface Loader<K, V> {

    V load(K key);
}
