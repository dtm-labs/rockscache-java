package io.github.dtm.cache.java;

import java.util.Collection;
import java.util.Map;

@FunctionalInterface
public interface Loader<K, V> {

    /**
     * @param key A collection without duplicated elements,
     *            so you can consider it as Set&lt;T&gt;
     * @return
     */
    Map<K, V> load(Collection<K> key);
}
