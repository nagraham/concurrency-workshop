package jcip.synchronizationbasics;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class FibonacciBenchmark {

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public static void callServiceMultithreaded() {
        CompletableFuture<Void> all = CompletableFuture.allOf(
                CompletableFuture.runAsync(FibonacciClient.newCountdownClient("A", 30, 50, "12586269025")),
                CompletableFuture.runAsync(FibonacciClient.newCountdownClient("B", 30, 9, "34")),
                CompletableFuture.runAsync(FibonacciClient.newCountdownClient("C", 30, 13, "233"))
        );
        all.join();
    }
}
