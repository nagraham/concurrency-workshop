package cache;

public interface Cache<Key, Value> {
    Value get(Key key);
}
