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
