package com.ph.phpictureback.manager.redisCache;

import com.ph.phpictureback.constant.RedisCacheConstant;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * 用户对图片分享的缓存
 */
@Component
public class PictureShareCache {
    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 图片点赞数的缓存
     * @param pictureId
     */
    public void addPictureShareCache(Long pictureId) {
        HashOperations<String, Long, Long> ops = redisTemplate.opsForHash();
        ops.increment(RedisCacheConstant.PICTURE_SHARE, pictureId, 1L);

    }


}
