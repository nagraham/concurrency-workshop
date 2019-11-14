package jcip;

import spark.Request;
import spark.Response;
import spark.Route;

import java.util.concurrent.atomic.AtomicReference;

import static spark.Spark.*;

public class SynchronizationBasics {

    public static void main(String[] args) {
        get("/fibonacci/:num", new UnsafeCachingFibonacci());
    }

    private static class UnsafeCachingFibonacci implements Route {
        private final AtomicReference<Integer> lastInput = new AtomicReference<>();
        private final AtomicReference<Integer> cachedFibonacci = new AtomicReference<>();

        @Override
        public Object handle(Request request, Response response) throws Exception {
            // we are assuming well formed input
            Integer input = Integer.parseInt(request.params("num"));

            if (input.equals(lastInput.get())) {
                return cachedFibonacci.get();
            } else {
                int result = fibonacci(input);
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
    private static int fibonacci(int num) {
        if (num < 0) {
            throw new IllegalArgumentException();
        } else if (num == 0) {
            return 0;
        }

        int a = 0;
        int b = 1;
        for (int i = 1; i < num; i++) {
            int next = a + b;
            a = b;
            b = next;
        }

        return b;
    }
}
