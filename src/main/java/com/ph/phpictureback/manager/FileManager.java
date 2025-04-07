package com.ph.phpictureback.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.config.CosClientConfig;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.picture.UploadPictureDto;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
@Deprecated
public class FileManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;


    /**
     * 上传图片
     *
     * @param multipartFile    图片文件
     * @param uploadPathPrefix 图片的路劲前缀
     * @return
     */
    public UploadPictureDto uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //检验文件
        vaildPicture(multipartFile);
        String filename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(filename);
        String uuid = RandomUtil.randomString(8);
        //上传文件的名字
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        //上传文件的路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //得到图片的信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
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
            return uploadPicture;

        } catch (IOException e) {
            log.error("upload picture error");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("delete file error");
                }
            }
        }
    }

    /**
     * 校验图片
     *
     * @param multipartFile
     */
    private void vaildPicture(MultipartFile multipartFile) {
        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片信息不存在");
        }
        //校验大小
        long fileSize = multipartFile.getSize();
        long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "图片过大");
        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        List<String> listSuffix = Arrays.asList("jpg", "png", "ico", "webp", "jpeg");
        ThrowUtils.throwIf(!listSuffix.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "图片类型错误");
    }


    /**
     * 从url下载图片
     *
     * @param url              图片文件
     * @param uploadPathPrefix 图片的路劲前缀
     * @return
     */
    public UploadPictureDto uploadPictureByUrl(String url, String uploadPathPrefix) {
        //检验文件
        validPicture(url);
        String filename = FileUtil.mainName(url);
        String suffix = FileUtil.getSuffix(filename);
        String uuid = RandomUtil.randomString(8);
        //上传文件的名字
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        //上传文件的路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            //下载文件
            HttpUtil.downloadFile(url, file);

            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            //得到图片的信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
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
            return uploadPicture;

        } catch (IOException e) {
            log.error("upload picture error");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("delete file error");
                }
            }
        }
    }

    /**
     * 校验url链接
     * @param fileUrl
     */
    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        try {
            // 1. 验证 URL 格式
            new URL(fileUrl); // 验证是否是合法的 URL
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 2. 校验 URL 协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

}
