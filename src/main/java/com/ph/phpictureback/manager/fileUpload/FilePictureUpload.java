package com.ph.phpictureback.manager.fileUpload;

import cn.hutool.core.io.FileUtil;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.manager.DownloadFileTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends DownloadFileTemplate {
    @Override
    public void changePictureType(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);

    }

    @Override
    public String getOriginName(Object inputSource) {
        return ((MultipartFile)inputSource).getOriginalFilename();
    }

    @Override
    public void vaildPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片信息不存在");
        }
        //校验大小
        long fileSize = (multipartFile).getSize();
        long ONE_M = 1024 * 1024L;
        //todo 图片的最大上传不能超过10MB
        ThrowUtils.throwIf(fileSize > 10 * ONE_M, ErrorCode.PARAMS_ERROR, "图片过大");
        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix((multipartFile).getOriginalFilename());
        List<String> listSuffix = Arrays.asList("jpg", "png", "ico", "webp", "jpeg");
        ThrowUtils.throwIf(!listSuffix.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "图片类型错误");
    }
}
