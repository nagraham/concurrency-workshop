package cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

/**
 * A basic cache implementation. Inspired by the implementation in Java Concurrency in Practice, section 5.6.
 *
 * This is an in-memory cache. There is no eviction policy (I plan to create other versions of a cache that
 * have an eviction policy later on). Clients provider a Function that takes in a Key as input and is able
 * to do the necessary work to generate a value that will be cached.
 *
 * @param <Key> The key type used to key values in the cache
 * @param <Value> The value type the cache will return
 */
public class BasicCache<Key, Value> implements Cache<Key, Value> {
    private static final Logger log = LoggerFactory.getLogger(BasicCache.class);

    private Function<Key, Value> producer;
    private Map<Key, CompletableFuture<Value>> cache;

    public BasicCache(Function<Key, Value> producer) {
        this.producer = producer;
        this.cache = new ConcurrentHashMap<>();
    }

    public Value get(Key key) {
        cache.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> producer.apply(k)));
        Value value = null;
        try {
            value = cache.get(key).get();
        } catch (ExecutionException ex) {
            log.error("Unexpected exception occurred getting cache value key={}", key, ex.getCause());
            throw new RuntimeException(ex.getCause());
        } catch (InterruptedException ex) {
            log.error("Interrupted!");
            Thread.currentThread().interrupt();
        }
        return value;
    }
}
