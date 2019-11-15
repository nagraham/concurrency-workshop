package jcip;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class SynchronizationBasicsClientRunnable implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SynchronizationBasicsClientRunnable.class);

    private OkHttpClient client;
    private Supplier<Integer> inputSupplier;

    public static void main(String[] args) {
        SynchronizationBasicsClientRunnable runner = new SynchronizationBasicsClientRunnable(new CountSupplier(3, 6));

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.submit(runner);
        executorService.shutdown();
    }

    public SynchronizationBasicsClientRunnable(Supplier<Integer> inputSupplier) {
        this.client = new OkHttpClient();
        this.inputSupplier = inputSupplier;
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
                log.info("Called /fibonacci; input={} result={}", input, response.body().string());
                input = inputSupplier.get();
            } catch (IOException exception) {
                log.error("Error calling /fibonacci with OkHttp client", exception);
                throw new RuntimeException(exception);
            }
        }
    }

    private static class CountSupplier implements Supplier<Integer> {

        private int count;
        private int input;

        public CountSupplier(int count, int input) {
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
