package cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicCacheTest {

    private static final int NUMBER_OF_TEST_THREADS = 5;

    @Nested
    @DisplayName("Single Threaded")
    class SingleThreaded {

        @Mock
        Function<String, Integer> mockFunction;

        Cache<String, Integer> numberCache;

        @BeforeEach
        void setup() {
            numberCache = new BasicCache<>(mockFunction);
        }

        @Test
        void whenGetCalled_andItemNotCached_callsFunction() {
            when(mockFunction.apply("A")).thenReturn(42);

            numberCache.get("A");

            verify(mockFunction, atMostOnce()).apply("A");
        }

        @Test
        void whenGetCalled_andItemNotCached_returnsValue() {
            when(mockFunction.apply("A")).thenReturn(42);

            Integer result = numberCache.get("A");

            assertThat(result, is(42));
        }

        @Test
        void whenGetCalled_andItemIsCached_doesNotCallFunction() {
            when(mockFunction.apply("A")).thenReturn(42);

            numberCache.get("A");
            numberCache.get("A");

            verify(mockFunction, atMostOnce()).apply("A");
        }

        @Test
        void whenGetCalled_andItemIsCached_returnsValue() {
            when(mockFunction.apply("A")).thenReturn(42);

            numberCache.get("A");
            Integer result = numberCache.get("A");

            assertThat(result, is(42));
        }

        @Test
        void withMultipleValuesInCache_getsAnyItem() {
            when(mockFunction.apply("A")).thenReturn(1);
            when(mockFunction.apply("B")).thenReturn(2);
            when(mockFunction.apply("C")).thenReturn(3);

            List<Integer> list = Arrays.asList(
                    numberCache.get("A"),
                    numberCache.get("B"),
                    numberCache.get("C")
            );

            assertThat(list, contains(1, 2, 3));
        }
    }


    @Nested
    @DisplayName("Multi Threaded")
    class MultiThreaded {

        @Mock
        Function<String, Integer> mockFunction;

        Cache<String, Integer> numberCache;

        @BeforeEach
        void setup() {
            numberCache = new BasicCache<>(mockFunction);
        }

        /**
         * NOTE: Using TDD, I validated a non-threadsafe implementation of the cache will cause this
         * test to fail almost every time. The mock function would be invoked 2-3 times. Occasionally
         * the succeeded, probably due to luck of the draw -- for that reason, the test is repeated 3 times.
         */
        @RepeatedTest(3)
        void whenCalledByMultipleThreads() {
            when(mockFunction.apply("A")).thenReturn(1);

            runOnMultipleThreads(NUMBER_OF_TEST_THREADS, () -> {
                for (int i = 0; i < 10; i++) {
                    numberCache.get("A");
                }
            });

            verify(mockFunction, atMostOnce()).apply("A");
        }
    }

    /**
     * Execute a runnable on a given number of threads. The purpose of this function is generally to
     * attempt to focus work on a single point of code, to generate a race condition or deadlock.
     */
    private void runOnMultipleThreads(int numOfThreads, Runnable runnable) {
        // Use this latch to ensure threads don't get a head start
        CountDownLatch startThreadLatch = new CountDownLatch(1);

        // create N threads; all wait on the latch (so they start at the same time), and then execute the runnable
        List<CompletableFuture<Void>> completableFutureList = new ArrayList<>();
        for (int i = 0; i < numOfThreads; i++) {
            completableFutureList.add(CompletableFuture.runAsync(() -> {
                try {
                    startThreadLatch.await();
                } catch (InterruptedException interrupt) {
                    // we can't throw a checked exception from a lambda; so we restore the
                    // interrupt status on the current thread (Java Concurrency in Practice, ch 5.4)
                    Thread.currentThread().interrupt();
                }

                runnable.run();
            }));
        }

        // start threads
        startThreadLatch.countDown();

        // block until all threads are complete
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0])).join();
    }

}