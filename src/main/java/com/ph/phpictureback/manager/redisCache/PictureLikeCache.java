package com.ph.phpictureback.manager.redisCache;

import com.ph.phpictureback.constant.RedisCacheConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * 用户对图片点赞表的缓存
 */
@Component
public class PictureLikeCache {
    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 图片点赞数的缓存
     * @param pictureId
     */
    public void addPictureLikeCache(Long pictureId) {
        HashOperations<String, Long, Long> ops = redisTemplate.opsForHash();
        //即使key不存在，会自动初始化
        ops.increment(RedisCacheConstant.PICTURE_LIKE, pictureId, 1L);

    }


    /**
     * 图片的取消点赞
     * @param pictureId
     */
    public void deletePictureLikeCache(Long pictureId) {
        HashOperations<String, Long, Long> ops = redisTemplate.opsForHash();
        if (!ops.hasKey(RedisCacheConstant.PICTURE_LIKE, pictureId)) {
            ops.put(RedisCacheConstant.PICTURE_LIKE, pictureId, -1L);
        } else {
            ops.increment(RedisCacheConstant.PICTURE_LIKE, pictureId, -1L);
        }
    }
}
