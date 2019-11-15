package jcip.synchronizationbasics;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * This client code calls FibonacciService repeatedly with multiple threads to test the thread safety.
 *
 * The unsynchronized code fails quite often. Over the course of 90 concurrent calls to the service, it could return
 * an incorrect result anywhere from 1 - 10 times. In fact, after running it 15 times, it never had a run where all
 * 90 succeeded! This would seem to drive home the point that unsynchronized code in the presence of concurrency is
 * VERY broken.
 *
 * [pool-1-thread-1] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=12586269025, expected=34
 * [pool-1-thread-1] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=12586269025, expected=34
 * [pool-1-thread-2] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=34, expected=12586269025
 * [pool-1-thread-3] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=233, expected=12586269025
 * [pool-1-thread-3] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=233, expected=34
 * [pool-1-thread-2] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=34, expected=233
 * [pool-1-thread-3] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=233, expected=34
 * [pool-1-thread-2] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=34, expected=233
 * [pool-1-thread-2] WARN jcip.synchronizationbasics.SynchronizationBasicsClientRunnable - Return value is incorrect: result=34, expected=233
 */
public class FibonacciClient implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(FibonacciClient.class);

    public static void main(String[] args) {
        FibonacciClient runnableA = newCountdownClient("A", 30, 50, "12586269025");
        FibonacciClient runnableB = newCountdownClient("B", 30, 9, "34");
        FibonacciClient runnableC = newCountdownClient("C", 30, 13, "233");

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(runnableA);
        executorService.submit(runnableB);
        executorService.submit(runnableC);
        executorService.shutdown();
    }

    private static FibonacciClient newCountdownClient(
            String label,
            int count,
            int input,
            String expectedResult
    ) {
        return new FibonacciClient(label, expectedResult, new CountDownSupplier(count, input));
    }

    private OkHttpClient client;
    private String label;
    private Supplier<Integer> inputSupplier;
    private String expectedResult;

    public FibonacciClient(String label, String expectedResult, Supplier<Integer> inputSupplier) {
        this.client = new OkHttpClient();
        this.label = label;
        this.inputSupplier = inputSupplier;
        this.expectedResult = expectedResult;
    }

    @Override
    public void run() {
        Integer input = inputSupplier.get();
        while (input != null) {
            Request httpRequest = new Request.Builder()
                    .url("http://localhost:4567/fibonacci/" + input)
                    .get()
                    .build();

            try {
                Response response = client.newCall(httpRequest).execute();
                String result = response.body().string();

                log.debug("{} called /fibonacci; input={} result={}", label, input, result);
                if (!expectedResult.equals(result)) {
                    log.warn("Return value is incorrect: result={}, expected={}", expectedResult, result);
                }

                input = inputSupplier.get();
            } catch (IOException exception) {
                log.error("Error calling /fibonacci with OkHttp client", exception);
                throw new RuntimeException(exception);
            }
        }
    }

    private static class CountDownSupplier implements Supplier<Integer> {
        private int count;
        private int input;

        public CountDownSupplier(int count, int input) {
            this.count = count;
            this.input = input;
        }

        @Override
        public Integer get() {
            if (count-- > 0) {
                return input;
            }
            return null;
        }
    }
}
