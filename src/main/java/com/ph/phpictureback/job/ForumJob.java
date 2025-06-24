package com.ph.phpictureback.job;

import cn.hutool.core.util.ObjectUtil;
import com.ph.phpictureback.constant.RedisCacheConstant;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.service.ForumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ForumJob {
    @Resource
    private ForumService forumService;
    @Resource
    private RedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 8-22/2 * * ?")
    public void ForumLikeJob() {
        HashOperations ops = redisTemplate.opsForHash();

        Set<Object> keys = ops.keys(RedisCacheConstant.FORUM_LIKE);
        if(ObjectUtil.isEmpty(keys)){
            return;
        }

        List<Long> forumIdList = keys.stream()
                .map(key-> ((Number) key).longValue())
                .collect(Collectors.toList());


        try {
            forumIdList.stream().forEach(forumId->{
                Object likeNum = ops.get(RedisCacheConstant.FORUM_LIKE, forumId);
                long likeCount = ((Number) likeNum).longValue();
                boolean update = forumService.lambdaUpdate()
                        .eq(Forum::getId, forumId)
                        .setSql("likeCount = likeCount +" + likeCount)
                        .update();
                if (!update) {
                    log.error("图片点赞数更新失败");
                }
                //数据填充之后，清除数据
                ops.delete(RedisCacheConstant.FORUM_LIKE,forumId);
            });
        } catch (Exception e) {
            log.error("图片点赞数更新失败",e);
        }
    }
    @Scheduled(cron = "0 0 8-22/2 * * ?")
    public void ForumShareJob() {
        HashOperations ops = redisTemplate.opsForHash();

        Set<Object> keys = ops.keys(RedisCacheConstant.FORUM_SHARE);
        if(ObjectUtil.isEmpty(keys)){
            return;
        }

        List<Long> forumIdList = keys.stream()
                .map(key-> ((Number) key).longValue())
                .collect(Collectors.toList());


        try {
            forumIdList.stream().forEach(forumId->{
                Object ShareNum = ops.get(RedisCacheConstant.FORUM_SHARE, forumId);
                long shareCount = ((Number) ShareNum).longValue();
                boolean update = forumService.lambdaUpdate()
                        .eq(Forum::getId, forumId)
                        .setSql("shareCount = shareCount +" + shareCount)
                        .update();
                if (!update) {
                    log.error("图片点赞数更新失败");
                }
                //数据填充之后，清除数据
                ops.delete(RedisCacheConstant.FORUM_SHARE,forumId);
            });
        } catch (Exception e) {
            log.error("图片点赞数更新失败",e);
        }
    }

    @Scheduled(cron = "0 0 8-22/2 * * ?")
    public void ForumViewJob() {
        HashOperations ops = redisTemplate.opsForHash();

        Set<Object> keys = ops.keys(RedisCacheConstant.FORUM_VIEW);
        if(ObjectUtil.isEmpty(keys)){
            return;
        }

        List<Long> forumIdList = keys.stream()
                .map(key-> ((Number) key).longValue())
                .collect(Collectors.toList());


        try {
            forumIdList.stream().forEach(forumId->{
                Object viewNum = ops.get(RedisCacheConstant.FORUM_VIEW, forumId);
                long viewCount = ((Number) viewNum).longValue();
                boolean update = forumService.lambdaUpdate()
                        .eq(Forum::getId, forumId)
                        .setSql("viewCount = viewCount +" + viewCount)
                        .update();
                if (!update) {
                    log.error("图片点赞数更新失败");
                }
                //数据填充之后，清除数据
                ops.delete(RedisCacheConstant.FORUM_VIEW,forumId);
            });
        } catch (Exception e) {
            log.error("图片点赞数更新失败",e);
        }
    }
}
