package com.ph.phpictureback;

import cn.hutool.core.util.ObjectUtil;
import com.ph.phpictureback.constant.RedisCacheConstant;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
class PhPictureBackApplicationTests {

    @Resource
    private PictureService pictureService;

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {

        HashOperations ops = redisTemplate.opsForHash();

        Set<Object> keys = ops.keys(RedisCacheConstant.PICTURE_LIKE);
        if(ObjectUtil.isEmpty(keys)){
            return;
        }

        List<Long> pictuireIdList = keys.stream()
                .map(key-> ((Number) key).longValue())
                .collect(Collectors.toList());


        try {
            pictuireIdList.stream().forEach(pictureId->{
                Object likeNum = ops.get(RedisCacheConstant.PICTURE_LIKE, pictureId);
                long likeCount = ((Number) likeNum).longValue();
                boolean update = pictureService.lambdaUpdate()
                        .eq(Picture::getId, pictureId)
                        .setSql("likeCount = likeCount +" + likeCount)
                        .update();
                if (!update) {
    //                log.error("图片点赞数更新失败");
                    System.out.println("图片点赞数更新失败");
                }
                //数据填充之后，清除数据
                ops.delete(RedisCacheConstant.PICTURE_LIKE,pictureId);
            });
        } catch (Exception e) {
            System.out.println("失败"+e);
        }

    }

    @Test
    void testView(){
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
//                    log.error();
                    System.out.println("图片浏览数数更新失败");
                }

                //数据填充之后，清除数据
                ops.delete(RedisCacheConstant.PICTURE_VIEW,pictureId);
            });
        } catch (Exception e) {

            System.out.println("图片浏览数更新失败"+e);
        }
    }

}
