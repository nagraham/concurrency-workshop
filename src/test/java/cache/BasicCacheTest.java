package cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
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
}