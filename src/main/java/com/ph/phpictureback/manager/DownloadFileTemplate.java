package com.ph.phpictureback.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.ph.phpictureback.config.CosClientConfig;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.model.dto.picture.UploadPictureDto;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public abstract class DownloadFileTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;


    /**
     * 上传图片
     *
     * @param inputSource      图片文件 / url
     * @param uploadPathPrefix 图片的路劲前缀
     * @return
     */
    public UploadPictureDto uploadPicture(Object inputSource, String uploadPathPrefix) {
        //1.检验文件
        vaildPicture(inputSource);
        //2.上传的地址
        // 上传文件的名字
        String filename = getOriginName(inputSource);
        String suffix = FileUtil.getSuffix(filename);
        String uuid = RandomUtil.randomString(8);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        //上传文件的路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            //3.创建临时文件
            file = File.createTempFile(uploadPath, null);
            //处理文件来源
            changePictureType(inputSource, file);
            //4.上传
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //5.得到图片的信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //将图片转化为webp
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                //webp格式图的信息
                CIObject webpCiObject = objectList.get(0);
                //默认webp格式的图片 = 缩略图的信息
                CIObject thumbnailUrlCiObject = webpCiObject;
                //需要缩略时，才进行缩略
                if (objectList.size() > 1) {
                    //缩略图的信息
                    thumbnailUrlCiObject = objectList.get(1);
                }

                //返回转化webp图的信息
                return getUploadPictureDto(uploadFileName, webpCiObject, thumbnailUrlCiObject, imageInfo);
            }
            //返回 原图信息
            return getUploadPictureDto(uploadPath, file, uploadFileName, imageInfo);

        } catch (IOException e) {
            log.error("upload picture error");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            //6.删除文件
            deleteFile(file);
        }
    }

    /**
     * 格式转化为webp的方法
     *
     * @param uploadFileName
     * @param webpCiObject
     * @return
     */
    private UploadPictureDto getUploadPictureDto(String uploadFileName, CIObject webpCiObject, CIObject thumbnailUrlCiObject, ImageInfo imageInfo) {
        int width = webpCiObject.getWidth();
        int height = webpCiObject.getHeight();
        double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        //设置图片的基本信息
        UploadPictureDto uploadPicture = new UploadPictureDto();
        uploadPicture.setPicName(uploadFileName);
        uploadPicture.setPicSize(webpCiObject.getSize().longValue());
        uploadPicture.setPicWidth(width);
        uploadPicture.setPicHeight(height);
        uploadPicture.setPicScale(scale);
        uploadPicture.setPicFormat(webpCiObject.getFormat());
        uploadPicture.setPicColor(imageInfo.getAve());
        //格式转化后图片的地址
        uploadPicture.setUrl(cosClientConfig.getHost() + "/" + webpCiObject.getKey());
        //压缩后的图片地址
        uploadPicture.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailUrlCiObject.getKey());

        return uploadPicture;
    }

    /**
     * 变化图片，从url下载，还是直接获取图片
     *
     * @param inputSource
     * @param file
     */
    public abstract void changePictureType(Object inputSource, File file) throws IOException;

    /**
     * 获取图片名称
     *
     * @param inputSource
     * @return
     */
    public abstract String getOriginName(Object inputSource);

    /**
     * 校验图片
     *
     * @param inputSource
     */
    public abstract void vaildPicture(Object inputSource);

    /**
     * 删除文件
     *
     * @param file
     */
    private static void deleteFile(File file) {
        if (file != null) {
            boolean delete = file.delete();
            if (!delete) {
                log.error("delete file error");
            }
        }
    }

    /**
     * 封装图片信息
     *
     * @param uploadPath
     * @param file
     * @param uploadFileName
     * @param imageInfo
     * @return
     */
    private UploadPictureDto getUploadPictureDto(String uploadPath, File file, String uploadFileName, ImageInfo imageInfo) {

        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        double scale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        //设置图片的基本信息
        UploadPictureDto uploadPicture = new UploadPictureDto();
        uploadPicture.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPicture.setPicName(uploadFileName);
        uploadPicture.setPicSize(FileUtil.size(file));
        uploadPicture.setPicWidth(width);
        uploadPicture.setPicHeight(height);
        uploadPicture.setPicScale(scale);
        uploadPicture.setPicFormat(imageInfo.getFormat());
        uploadPicture.setPicColor(imageInfo.getAve());
        return uploadPicture;
    }

}
