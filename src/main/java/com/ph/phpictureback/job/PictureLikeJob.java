package com.ph.phpictureback.job;

import cn.hutool.core.util.ObjectUtil;
import com.ph.phpictureback.constant.RedisCacheConstant;
import com.ph.phpictureback.model.entry.Picture;
import com.ph.phpictureback.model.enums.ReviewStatusEnum;
import com.ph.phpictureback.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PictureLikeJob {
    @Resource
    private PictureService pictureService;
    @Resource
    private RedisTemplate redisTemplate;

    @Resource(name = "SyncExecutorService")
    private ExecutorService executorService;

    @Scheduled(cron = "0 0 8-22/2 * * ?")
    public void pictureLikeJob() {
        HashOperations ops = redisTemplate.opsForHash();
        String hashKey = RedisCacheConstant.PICTURE_LIKE;
        //每次扫描1000条数据，使用渐进式扫描
        ScanOptions options = ScanOptions.scanOptions().count(1000).build();
        //使用scan进行渐进式扫描，每次只扫描 options定义的数量，返回一个 游标 ，让下一次扫描从游标继续，知道遍历所有数据
        Cursor<Map.Entry<Long,Long>> cursor = ops.scan(hashKey, options);
        //创建批量处理的集合
        List<Map.Entry<Long,Long>> batch = new ArrayList<>(100);
        try {
            //遍历游标，获取数据
            while (cursor.hasNext()){
                batch.add(cursor.next());
                if(batch.size() >= 100){
                    //批量处理，避免阻塞
                    List<Map.Entry<Long, Long>> finalBatch = batch;
                    //使用线程池异步处理
                    executorService.execute(()->processBatch(finalBatch,ops,hashKey));
                    //批量处理完成，重新设置值
                    batch =new ArrayList<>(100);
                }
            }
            //处理剩余的数据
            if(!batch.isEmpty()){
                List<Map.Entry<Long, Long>> finalBatch1 = batch;
                executorService.execute(()->processBatch(finalBatch1,ops,hashKey));
            }
        }catch (Exception e){
            log.error("扫描redis的数据失败",e);
        }finally {
            if(cursor!=null){
                try{
                    cursor.close();
                }catch (Exception e){
                    log.error("关闭redis游标失败",e);
                }
            }
        }
    }

    /**
     * 批量处理单次数据
     * @param batch
     * @param ops
     * @param hashKey
     */
    private void processBatch(List<Map.Entry<Long,Long>> batch,HashOperations ops,String hashKey){
        //收集需要更新的数据
        HashMap<Long, Long> map = new HashMap<>();
        for (Map.Entry<Long, Long> entry : batch) {
            //转换为long类型，防止redis转换报错
            Number key = entry.getKey();
            Number value = entry.getValue();
            map.put(key.longValue(),value.longValue());
        }
        //批量更新数据库
        boolean update=pictureService.batchUpdatePictureLike(map);
        //更新成功之后删除redis的key
        if(update){
            ops.delete(hashKey,map.keySet().toArray()); //批量删除
        }else{
            log.error("图片点赞数更新失败");
        }
    }
}
