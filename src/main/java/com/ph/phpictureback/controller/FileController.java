package com.ph.phpictureback.controller;

import cn.hutool.core.io.FileUtil;
import com.ph.phpictureback.annotation.AuthCheck;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.constant.UserConstant;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.manager.CosManager;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 文件上传
 */
@RestController
@RequestMapping("/upload")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 上传文件
     *
     * @param multipartFile 文件
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN)
    @PostMapping("/test/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //得到文件名
        String filename = multipartFile.getOriginalFilename();
        //存储的路径
        String filePath = String.format("/test/%s", filename);
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            //上传文件
            cosManager.putObject(filePath, file);
            //返回文件的地址
            return ResultUtils.success(filePath);
        } catch (IOException e) {
            log.error("上传文件失败:{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败");
        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("删除临时文件失败：{}", filePath);
                }
            }
        }

    }


    /**
     * 下载文件
     *
     * @param filePath 文件的标识
     * @param response 返回给前端的信息
     */
    @AuthCheck(mustRole = UserConstant.ADMIN)
    @GetMapping("/test/download")
    public void downloadFile(String filePath, HttpServletResponse response) throws IOException {
        COSObjectInputStream objectContent = null;
        //下载文件
        try {
            COSObject downloadFile = cosManager.getObject(filePath);
            objectContent = downloadFile.getObjectContent();
            //得到下载流
            byte[] byteArray = IOUtils.toByteArray(objectContent);
            //设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filePath);
            //写入响应
            response.getOutputStream().write(byteArray);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("下载文件失败:{}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载文件失败");
        } finally {
            if (objectContent != null) {
                objectContent.close();
            }
        }
    }
}
