package jcip.synchronizationbasics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

import static spark.Spark.*;

/**
 * A basic service that returns the Nth number in the Fibonacci sequence. It will cache the previous
 * result.
 */
public class FibonacciService {
    private static Logger logger = LoggerFactory.getLogger(FibonacciService.class);

    public static void main(String[] args) {
        logger.info("Starting my REST server!");
        get("/fibonacci/:num", new ImmutableCachingFibonacci());
    }

    /**
     * A handler that caches and returns the Fibonacci sequence number without thread safety.
     *
     * Runs (in millis): 1250, 947, 842, 879, 844, 821, 801 (912 avg)
     */
    private static class UnsafeCachingFibonacci implements Route {
        private final AtomicReference<Integer> lastInput = new AtomicReference<>();
        private final AtomicReference<BigInteger> cachedFibonacci = new AtomicReference<>();

        @Override
        public Object handle(Request request, Response response) throws Exception {
            // we are assuming well formed input
            Integer input = Integer.parseInt(request.params("num"));

            if (input.equals(lastInput.get())) {
                logger.info("Cache hit! input={}", input);
                return cachedFibonacci.get();
            } else {
                BigInteger result = fibonacci(input);
                lastInput.set(input);
                cachedFibonacci.set(result);
                return result;
            }
        }
    }

    /**
     * A handler that caches and returns the Fibonacci sequence number with thread safety
     * by synchronizing the entire method.
     *
     * Runs (ms): 1198, 814, 820, 813, 1038, 814, 824 (903 avg)
     */
    private static class OverlySyncedCachingFibonacci implements Route {
        private final AtomicReference<Integer> lastInput = new AtomicReference<>();
        private final AtomicReference<BigInteger> cachedFibonacci = new AtomicReference<>();

        @Override
        public synchronized Object handle(Request request, Response response) throws Exception {
            // we are assuming well formed input
            Integer input = Integer.parseInt(request.params("num"));

            if (input.equals(lastInput.get())) {
                logger.info("Cache hit! input={}", input);
                return cachedFibonacci.get();
            } else {
                BigInteger result = fibonacci(input);
                lastInput.set(input);
                cachedFibonacci.set(result);
                return result;
            }
        }
    }

    /**
     * This works -- no issues due to race conditions. And the version number appeared to increase monotonically
     * across cache hits. Although I am sure my benchmark is too scrappy to make solid conclusions on, it is clear
     * that on average it has better performance than the synchronized version.
     *
     * Runs (ms): 1105, 821, 897, 818, 784, 848, 806 (868 avg)
     */
    private static class ImmutableCachingFibonacci implements Route {
        private volatile ImmutableOneFibonacciCache cache = ImmutableOneFibonacciCache.emptyCache();

        @Override
        public Object handle(Request request, Response response) throws Exception {
            // we are assuming well formed input
            Integer input = Integer.parseInt(request.params("num"));

            // we can safely do the null check b/c the thread stores its vision of "lastFibonacci" into a local
            // variable, which gets stashed in the thread's stack; you could also store the cache itself into a local
            // variable, and store the result of ".update()" into the instance variable, which would make that new
            // version visible to all threads
            BigInteger fibonacci = cache.getLastFibonacci(input);
            if (fibonacci == null) {
                fibonacci = fibonacci(input);

                // b/c the "cache" is volatile, when a thread calls update, it is called on the LAST written version
                cache = cache.update(input, fibonacci);
            } else {
                logger.info("Cache hit! input={}, cache.version={}", input, cache.version);
            }

            return fibonacci;
        }
    }

    /**
     * Stores a single key/value pair, and encapsulates the logic to retrieve the value.
     *
     * Because this object is immutable, if it is stored in a volatile variable, the "check-then-get"
     * compound operation is made thread safe and does not require synchronization!
     *
     * The immutability of the class guarantees each thread has its own copy: if thread A
     * is performing the "check-then-get", thread B can't overwrite it and cause a race condition.
     *
     * Storing in a volatile variable guarantees visibility of the last written immutable cache.
     */
    private static class ImmutableOneFibonacciCache {
        private final Integer lastInput;
        private final BigInteger lastFibonacci;
        private final int version;

        private ImmutableOneFibonacciCache(Integer lastInput, BigInteger lastFibonacci, int version) {
            this.lastInput = lastInput;
            this.lastFibonacci = lastFibonacci;
            this.version = version;
        }

        public static ImmutableOneFibonacciCache emptyCache() {
            return new ImmutableOneFibonacciCache(null, null, 0);
        }

        public ImmutableOneFibonacciCache update(Integer newInput, BigInteger newFibonacci) {
            return new ImmutableOneFibonacciCache(newInput, newFibonacci, this.version + 1);
        }

        public BigInteger getLastFibonacci(Integer input) {
            if (lastInput == null || !lastInput.equals(input)) {
                return null;
            } else {
                return lastFibonacci;
            }
        }
    }

    /**
     * Get the n'th fibonacci
     *
     * @param num
     * @return
     */
    private static BigInteger fibonacci(int num) {
        if (num < 0) {
            throw new IllegalArgumentException();
        } else if (num == 0) {
            return BigInteger.ZERO;
        }

        BigInteger a = BigInteger.ZERO;
        BigInteger b = BigInteger.ONE;
        for (int i = 1; i < num; i++) {
            BigInteger next = a.add(b);
            a = b;
            b = next;
        }

        return b;
    }
}
