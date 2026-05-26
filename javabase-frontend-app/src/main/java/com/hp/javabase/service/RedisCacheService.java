package com.hp.javabase.service;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisCacheService {

    private final RedissonClient redissonClient;

    public RedisCacheService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> void set(String key, T value) {
        redissonClient.getBucket(key).set(value);
    }

    public <T> void set(String key, T value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            set(key, value);
            return;
        }
        redissonClient.getBucket(key).set(value, ttl);
    }

    public <T> T get(String key, Class<T> clazz) {
        Object value = redissonClient.getBucket(key).get();
        if (value == null) {
            return null;
        }
        return clazz.cast(value);
    }

    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public long remainTimeToLive(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.remainTimeToLive();
    }
}
