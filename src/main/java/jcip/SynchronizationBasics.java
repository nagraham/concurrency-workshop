package jcip;

import spark.Response;

import static spark.Spark.*;

public class SynchronizationBasics {

    public static void main(String[] args) {
        get("/fibonacci/:num", (request, response) -> {
            // we are assuming well formed input
            return fibonacci(Integer.parseInt(request.params("num"))) + "\n";
        });
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
