package io.github.dtm.cache.java;

import java.util.Collection;
import java.util.Map;

@FunctionalInterface
public interface BatchLoader<K, V> {

    Map<K, V> loadAll(Collection<K> keys);
}
