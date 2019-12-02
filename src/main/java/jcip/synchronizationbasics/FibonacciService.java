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
        get("/fibonacci/:num", new UnsafeCachingFibonacci());
    }

    /**
     * A handler that caches and returns the Fibonacci sequence number without thread safety.
     *
     * Benchmark:
     *   5.405 ±(99.9%) 0.089 ms/op [Average]
     *   (min, avg, max) = (5.372, 5.405, 5.430), stdev = 0.023
     *   CI (99.9%): [5.316, 5.494] (assumes normal distribution)
     */
    private static class UnsafeCachingFibonacci implements Route {
        private final AtomicReference<Integer> lastInput = new AtomicReference<>();
        private final AtomicReference<BigInteger> cachedFibonacci = new AtomicReference<>();

        @Override
        public Object handle(Request request, Response response) throws Exception {
            // we are assuming well formed input
            Integer input = Integer.parseInt(request.params("num"));

            if (input.equals(lastInput.get())) {
                logger.debug("Cache hit! input={}", input);
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
     * Benchmark (w/o logging)
     *   5.756 ±(99.9%) 0.589 ms/op [Average]
     *   (min, avg, max) = (5.619, 5.756, 6.017), stdev = 0.153
     *   CI (99.9%): [5.167, 6.345] (assumes normal distribution)
     */
    private static class OverlySyncedCachingFibonacci implements Route {
        private final AtomicReference<Integer> lastInput = new AtomicReference<>();
        private final AtomicReference<BigInteger> cachedFibonacci = new AtomicReference<>();

        @Override
        public synchronized Object handle(Request request, Response response) throws Exception {
            // we are assuming well formed input
            Integer input = Integer.parseInt(request.params("num"));

            if (input.equals(lastInput.get())) {
                logger.debug("Cache hit! input={}", input);
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
     * across cache hits.
     *
     * By comparing the JMH benchmark with the overly synchronized version, the code is moderately faster
     * (.2 ms on average), but with far less volatility (standard dev 0.041 ms compared to 0.153ms). It's
     * still slower than the unthread-safe version despite the lack of synchronization. Perhaps that is due
     * to the cost of allocating new objects?
     *
     * Benchmark:
     *   5.554 ±(99.9%) 0.156 ms/op [Average]
     *   (min, avg, max) = (5.506, 5.554, 5.607), stdev = 0.041
     *   CI (99.9%): [5.398, 5.710] (assumes normal distribution)
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
                logger.debug("Cache hit! input={}, cache.version={}", input, cache.version);
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
