package com.ph.phpictureback.manager.redisCache;

import com.ph.phpictureback.constant.RedisCacheConstant;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * 用户对帖子点赞表的缓存
 */
@Component
public class ForumCache {
    @Resource
    private RedisTemplate redisTemplate;


    /**
     * 帖子点赞数的缓存
     * @param forumId
     */
    public void addForumLikeCache(Long forumId) {
        HashOperations<String, Long, Long> ops = redisTemplate.opsForHash();
        if (!ops.hasKey(RedisCacheConstant.FORUM_LIKE, forumId)) {
            ops.put(RedisCacheConstant.FORUM_LIKE, forumId, 1L);
        } else {
            ops.increment(RedisCacheConstant.FORUM_LIKE, forumId, 1L);
        }
    }


    /**
     * 帖子的取消点赞
     * @param forumId
     */
    public void deleteForumLikeCache(Long forumId) {
        HashOperations<String, Long, Long> ops = redisTemplate.opsForHash();
        if (!ops.hasKey(RedisCacheConstant.FORUM_LIKE, forumId)) {
            ops.put(RedisCacheConstant.FORUM_LIKE, forumId, -1L);
        } else {
            ops.increment(RedisCacheConstant.FORUM_LIKE, forumId, -1L);
        }
    }


    /**
     * 帖子分享数的缓存
     * @param forumId
     */
    public void addForumShareCache(Long forumId) {
        HashOperations<String, Long, Long> ops = redisTemplate.opsForHash();
        if (!ops.hasKey(RedisCacheConstant.FORUM_SHARE, forumId)) {
            ops.put(RedisCacheConstant.FORUM_SHARE, forumId, 1L);
        } else {
            ops.increment(RedisCacheConstant.FORUM_SHARE, forumId, 1L);
        }
    }

    /**
     * 帖子浏览的缓存
     * @param forumId
     */
    public void addForumViewCache(Long forumId) {
        HashOperations<String, Long, Long> ops = redisTemplate.opsForHash();
        if(!ops.hasKey(RedisCacheConstant.FORUM_VIEW,forumId)){
            ops.put(RedisCacheConstant.FORUM_VIEW, forumId, 1L);
        }else{
            ops.increment(RedisCacheConstant.FORUM_VIEW, forumId, 1L);
        }
    }
    
    
}
