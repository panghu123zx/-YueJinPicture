package com.ph.phpictureback.manager;

import cn.hutool.core.io.FileUtil;
import com.ph.phpictureback.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 上传文件
 */
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 上传对象
     *
     * @param key  唯一标识
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 下载的文件
     * @return
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }


    /**
     * 上传对象,携带图片信息（数据万象处理）
     *
     * @param key  唯一标识
     * @param file 文件
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        //图片处理
        PicOperations picOperations = new PicOperations();
        //返回原图信息
        picOperations.setIsPicInfo(1);
        //图片类型的转化规则
        String webpKey = FileUtil.mainName(key) + ".png";
        // 添加图片处理规则  转化为webp格式
        List<PicOperations.Rule> ruleList = new LinkedList<>();
        PicOperations.Rule ruleSet = new PicOperations.Rule();
        ruleSet.setBucket(cosClientConfig.getBucket());
        ruleSet.setFileId(webpKey);
        ruleSet.setRule("imageMogr2/format/png");
        ruleList.add(ruleSet);
        //如果缩略图的大小大于 20kb 时，才进行缩略
        if (file.length() > 2 * 1024) {
            //缩略图的转化规则
            PicOperations.Rule thumbnailUrlRule = new PicOperations.Rule();
            //缩略图的文件名称
            String thumbnailUrlKey = FileUtil.mainName(key) + "_thumbnailUrl" + FileUtil.getSuffix(key);
            thumbnailUrlRule.setBucket(cosClientConfig.getBucket());
            thumbnailUrlRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 384, 384));
            thumbnailUrlRule.setFileId(thumbnailUrlKey);
            ruleList.add(thumbnailUrlRule);
        }
        //构造处理文件的参数
        picOperations.setRules(ruleList);
        //设置构造函数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 删除cos中的数据
     * @param key
     */
    public void  deleteObject(String key){
        cosClient.deleteObject(cosClientConfig.getBucket(),key);
    }

}
