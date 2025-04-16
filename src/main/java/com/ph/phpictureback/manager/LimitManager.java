package com.ph.phpictureback.manager;

import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

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
}
