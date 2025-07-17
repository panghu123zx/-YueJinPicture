package com.ph.phpictureback.manager.minio;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.ph.phpictureback.config.MinioConfig;

import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * minio
 */
@Component
@Slf4j
public class MinioManage {
    @Resource
    private MinioClient minioClient;
    @Resource
    private MinioConfig minioConfig;

    /**
     * 判断桶是否存在
     * @return 桶是否存在
     */
    public boolean existBucketName() {
        boolean exists;
        try {
            //判断桶是否存在
            String bucketName = minioConfig.getBucketName();
            exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if(!exists){
                //不存在就新建
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                exists=true;
            }
        }catch (Exception e){
            log.error("查看桶失败：",e);
            exists=false;
        }
        return exists;
    }

    /**
     * 新建桶
     * @return  创建存储bucket
     */
    public Boolean makeBucket() {
        try {
            //桶存在直接返回
            if(existBucketName()){
                return true;
            }
            String bucketName = minioConfig.getBucketName();
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("创建桶失败：",e);
            return false;
        }
    }

    /**
     * 移除桶
     * @return  删除存储bucket
     */
    public Boolean removeBucket() {
        String bucketName = minioConfig.getBucketName();
        if(!existBucketName()) return true;
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("移除桶失败：",e);
            return false;
        }
    }

    /**
     * 获取上传图片的零时签名
     * @param fileName 文件名称
     * @param time     时间
     * @return  获取上传临时签名
     */
    @SneakyThrows
    public Map getPolicy(String fileName, ZonedDateTime time) {
        PostPolicy postPolicy = new PostPolicy(minioConfig.getBucketName(), time);
        postPolicy.addEqualsCondition("key", fileName);
        try {
            Map<String, String> map = minioClient.getPresignedPostFormData(postPolicy);
            HashMap<String, String> map1 = new HashMap<>();
            map.forEach((k, v) -> {
                map1.put(k.replaceAll("-", ""), v);
            });
            map1.put("host", minioConfig.getUrl() + "/" + minioConfig.getBucketName());
            return map1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取全部bucket
     */
    public List<Bucket> getAllBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            log.error("获取所有的桶信息", e);
        }
        return null;
    }


    /**
     * @param objectName 对象名称
     * @return  获取上传文件的url
     */
    public String getFileUrl(String objectName) {
        try {
            GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS)
                    .build();
            return minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
        } catch (Exception e) {
            log.error("错误",e);
        }
        return null;
    }


    /**
     * 上传
     * @param file     文件
     * @param uploadPathPrefix 文件上传的前缀
     * @return  文件的地址
     */
    public String upload(MultipartFile file,String uploadPathPrefix) {
        // 使用putObject上传一个文件到存储桶中。
        boolean existed = existBucketName();
        //桶不存在，创建桶
        if(!existed) makeBucket();
        String bucketName = minioConfig.getBucketName();

        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String uuid = RandomUtil.randomString(8);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String uploadPath=String.format("/%s/%s",uploadPathPrefix,uploadFileName);
        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(uploadPath)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            inputStream.close();
            String url= minioConfig.getUrl() +"/" +minioConfig.getBucketName()+uploadPath;
            return url;
        } catch (Exception e) {
            log.error("上传文件失败：",e);
            return null;
        }
    }


    /**
     * @param objectName 对象名称
     * @param time       时间
     * @param timeUnit   时间单位
     * @return  根据filename获取文件访问地址
     */
    public String getUrl(String objectName, int time, TimeUnit timeUnit) {
        String url = null;
        try {
            url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .expiry(time, timeUnit).build());
        } catch (Exception e) {
            e.printStackTrace();
        } 
        return url;
    }

    /**
     * @return  description: 下载文件
     */
    public ResponseEntity<byte[]> download(String fileName) {
        ResponseEntity<byte[]> responseEntity = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = minioClient.getObject(GetObjectArgs.builder().bucket(minioConfig.getBucketName()).object(fileName).build());
            out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);
            //封装返回值
            byte[] bytes = out.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            try {
                headers.add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            headers.setContentLength(bytes.length);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setAccessControlExposeHeaders(Arrays.asList("*"));
            responseEntity = new ResponseEntity<byte[]>(bytes, headers, 200);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return responseEntity;
    }

}
