package com.ph.phpictureback.job;

import cn.hutool.core.util.ObjectUtil;
import com.ph.phpictureback.constant.RedisCacheConstant;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.enums.ReviewStatusEnum;
import com.ph.phpictureback.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PictureViewJob {
    @Resource
    private PictureService pictureService;
    @Resource
    private RedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 8-22/2 * * ?")
    public void pictureViewJob() {
        HashOperations ops = redisTemplate.opsForHash();
        Set<Object> keys = ops.keys(RedisCacheConstant.PICTURE_VIEW);
        if(ObjectUtil.isEmpty(keys)){
            return;
        }

        List<Long> pictuireIdList = keys.stream()
                .map(key-> ((Number) key).longValue())
                .collect(Collectors.toList());


        try {
            pictuireIdList.stream().forEach(pictureId->{
                Object viewSum = ops.get(RedisCacheConstant.PICTURE_VIEW, pictureId);
                long viewCount = ((Number) viewSum).longValue();
                boolean update = pictureService.lambdaUpdate()
                        .eq(Picture::getId, pictureId)
                        .setSql("viewCount = viewCount +" + viewCount)
                        .update();
                if (!update) {
                    log.error("图片浏览数数更新失败");
                }

                //数据填充之后，清除数据
                ops.delete(RedisCacheConstant.PICTURE_VIEW,pictureId);
            });
        } catch (Exception e) {
            log.error("图片浏览数更新失败",e);
        }


    }
}
