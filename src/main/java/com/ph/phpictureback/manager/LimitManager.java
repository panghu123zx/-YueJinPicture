package com.ph.phpictureback.manager;

import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器
 */

@Component
public class LimitManager {

    @Resource
    private RedissonClient redissonClient;

    public void RateLimit(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,10,1, RateIntervalUnit.SECONDS);
        boolean update = rateLimiter.tryAcquire(1);
        if(!update){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"请求过于频繁");
        }
    }

    /*
    加强版限流算法
    private final Map<String,RRateLimiter> limiterMap=new ConcurrentHashMap<>();

    private void  initRate(String key){
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,10,1, RateIntervalUnit.SECONDS);
        limiterMap.put(key,rateLimiter);
    }
    public void RateLimitMax(String key) {
        RRateLimiter rateLimiter = limiterMap.get(key);
        RRateLimiter rRateLimiter = limiterMap.computeIfAbsent(key, k -> {
            RRateLimiter rateLimiter1 = redissonClient.getRateLimiter(key);
            rateLimiter1.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.SECONDS);
            return rateLimiter1;
        });
        boolean update = rRateLimiter.tryAcquire(1);
        if(!update){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"请求过于频繁");
        }

    }*/
}
