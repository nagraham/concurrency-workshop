package jcip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

import static spark.Spark.*;

public class SynchronizationBasics {
    private static Logger logger = LoggerFactory.getLogger(SynchronizationBasics.class);

    public static void main(String[] args) {
        logger.info("Starting my REST server!");
        get("/fibonacci/:num", new UnsafeCachingFibonacci());
    }

    private static class UnsafeCachingFibonacci implements Route {
        private final AtomicReference<Integer> lastInput = new AtomicReference<>();
        private final AtomicReference<BigInteger> cachedFibonacci = new AtomicReference<>();

        @Override
        public Object handle(Request request, Response response) throws Exception {
            // we are assuming well formed input
            Integer input = Integer.parseInt(request.params("num"));

            if (input.equals(lastInput.get())) {
                logger.info("Cache hit! input={}", input);
                return cachedFibonacci.get() + "\n";
            } else {
                BigInteger result = fibonacci(input);
                lastInput.set(input);
                cachedFibonacci.set(result);
                return result + "\n";
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
