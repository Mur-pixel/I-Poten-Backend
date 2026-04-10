package com.cygnus.ipoten.redis_cache;

import java.time.Duration;

public interface RedisCacheService {
    <K, V> void setKeyAndValue(K key, V value);
    <K, V> void setKeyAndValue(K key, V value, Duration timeout);
    <T> T getValueByKey(String key, Class<T> clazz);
    void deleteByKey(String token);
}
