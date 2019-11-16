package util;

import org.slf4j.Logger;

import java.time.Instant;

public class Benchmark {

    public static void logTime(String label, Logger logger, Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        long end = System.nanoTime();
        logger.info("{} ran in {}ms ; {} nanos", label, (end - start) / 1000000L, end - start);
    }

}
