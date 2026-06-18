package com.lowcode.common.util;

import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedissonClient redissonClient;

    public <T> void set(String key, T value) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    public <T> void set(String key, T value, long timeout, TimeUnit unit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, timeout, unit);
    }

    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redissonClient.getBucket(key).expire(timeout, unit);
    }

    public long getExpire(String key) {
        return redissonClient.getBucket(key).remainTimeToLive();
    }

    public boolean hasKey(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public long incr(String key, long delta) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.addAndGet(delta);
    }

    public long decr(String key, long delta) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.addAndGet(-delta);
    }

    public <T> void hSet(String key, String hashKey, T value) {
        RMap<String, T> map = redissonClient.getMap(key);
        map.put(hashKey, value);
    }

    public <T> T hGet(String key, String hashKey) {
        RMap<String, T> map = redissonClient.getMap(key);
        return map.get(hashKey);
    }

    public <T> Map<String, T> hGetAll(String key) {
        RMap<String, T> map = redissonClient.getMap(key);
        return map.readAllMap();
    }

    public void hDel(String key, String... hashKeys) {
        RMap<String, ?> map = redissonClient.getMap(key);
        map.fastRemove(hashKeys);
    }

    public boolean hHasKey(String key, String hashKey) {
        RMap<String, ?> map = redissonClient.getMap(key);
        return map.containsKey(hashKey);
    }

    public long hIncr(String key, String hashKey, long delta) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.addAndGet(hashKey, delta);
    }

    public <T> void lSet(String key, T value) {
        RList<T> list = redissonClient.getList(key);
        list.add(value);
    }

    public <T> void lSet(String key, T value, long timeout, TimeUnit unit) {
        RList<T> list = redissonClient.getList(key);
        list.add(value);
        list.expire(timeout, unit);
    }

    public <T> List<T> lGet(String key) {
        RList<T> list = redissonClient.getList(key);
        return list.readAll();
    }

    public <T> List<T> lGet(String key, int start, int end) {
        RList<T> list = redissonClient.getList(key);
        return list.range(start, end);
    }

    public long lSize(String key) {
        RList<?> list = redissonClient.getList(key);
        return list.size();
    }

    public <T> void sSet(String key, T value) {
        RSet<T> set = redissonClient.getSet(key);
        set.add(value);
    }

    public <T> Set<T> sGet(String key) {
        RSet<T> set = redissonClient.getSet(key);
        return set.readAll();
    }

    public boolean sHasKey(String key, Object value) {
        RSet<?> set = redissonClient.getSet(key);
        return set.contains(value);
    }

    public long sSize(String key) {
        RSet<?> set = redissonClient.getSet(key);
        return set.size();
    }

    public void sRemove(String key, Object... values) {
        RSet<?> set = redissonClient.getSet(key);
        set.removeAll(java.util.Arrays.asList(values));
    }

    public <T> void zSet(String key, T value, double score) {
        RScoredSortedSet<T> sortedSet = redissonClient.getScoredSortedSet(key);
        sortedSet.add(score, value);
    }

    public <T> Collection<T> zGet(String key, int start, int end) {
        RScoredSortedSet<T> sortedSet = redissonClient.getScoredSortedSet(key);
        return sortedSet.valueRange(start, end);
    }

    public void zRemove(String key, Object... values) {
        RScoredSortedSet<?> sortedSet = redissonClient.getScoredSortedSet(key);
        sortedSet.removeAll(java.util.Arrays.asList(values));
    }

    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.tryLock(waitTime, leaseTime, unit);
    }

    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    public <T> RRateLimiter getRateLimiter(String key) {
        return redissonClient.getRateLimiter(key);
    }

    public boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent(value, timeout, unit);
    }

    public void deleteByPattern(String pattern) {
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(pattern);
        redissonClient.getKeys().delete(keys);
    }
}
