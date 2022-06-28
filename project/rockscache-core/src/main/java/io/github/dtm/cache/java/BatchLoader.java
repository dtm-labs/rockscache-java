package io.github.dtm.cache.java;

import java.util.Collection;
import java.util.Map;

@FunctionalInterface
public interface BatchLoader<T> {

    Map<String, T> loadAll(Collection<String> keys);
}
