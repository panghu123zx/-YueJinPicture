package com.ph.phpictureback.manager;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.function.Supplier;

public class RedissonLock {

    @Resource
    private RedissonClient redissonClient;
    public <T> T lockExecute(String lockKey, Supplier<T> supplier){
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            return supplier.get();
        }finally {
            lock.unlock();
        }
    }
}