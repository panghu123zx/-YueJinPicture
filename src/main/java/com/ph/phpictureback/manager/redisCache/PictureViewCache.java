package com.ph.phpictureback.manager.redisCache;

import com.ph.phpictureback.constant.RedisCacheConstant;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * 用户对图片的浏览
 */
@Component
public class PictureViewCache {
    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 图片浏览的缓存
     * @param pictureId
     */
    public void addPictureViewCache(Long pictureId) {
        HashOperations<String, Long, Long> ops = redisTemplate.opsForHash();
        ops.increment(RedisCacheConstant.PICTURE_VIEW, pictureId, 1L);

    }


}
