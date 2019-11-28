package cache;

import java.util.concurrent.ExecutionException;

public interface Cache<Key, Value> {
    Value get(Key key) throws InterruptedException, ExecutionException;
    Value getUnchecked(Key key);
}
