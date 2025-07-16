package com.ph.phpictureback.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.audioFile.AudioFileAddDto;
import com.ph.phpictureback.model.dto.audioFile.AudioFileQueryDto;
import com.ph.phpictureback.model.dto.audioFile.AudioFileUpdateDto;
import com.ph.phpictureback.model.entry.AudioFile;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.AudioFileVO;
import com.ph.phpictureback.service.AudioFileService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/audio")
public class AudioFileController {
    @Resource
    private UserService userService;

    @Resource
    private AudioFileService audioFileService;

    /**
     * 新增文件
     * @param multipartFile
     * @param audioFileAddDto
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addAudioFile(@RequestPart("file") MultipartFile multipartFile
            , AudioFileAddDto audioFileAddDto, HttpServletRequest request){
        ThrowUtils.throwIf(audioFileAddDto==null, ErrorCode.PARAMS_ERROR,"参数为空");
        User loginUser = userService.getLoginUser(request);
        Long audioFileId = audioFileService.addAudioFile(multipartFile, audioFileAddDto, loginUser);
        return ResultUtils.success(audioFileId);
    }

    /**
     * 更新文件信息
     * @param audioFileUpdateDto
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateAudioFile(@RequestBody AudioFileUpdateDto audioFileUpdateDto,
                                                 HttpServletRequest request){
        ThrowUtils.throwIf(audioFileUpdateDto==null, ErrorCode.PARAMS_ERROR,"参数为空");
        User loginUser = userService.getLoginUser(request);
        Boolean result = audioFileService.updateAudioFile(audioFileUpdateDto, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 删除文件
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteAudioFile(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        ThrowUtils.throwIf(deleteRequest==null || deleteRequest.getId()<=0, ErrorCode.PARAMS_ERROR,"参数为空");
        User loginUser = userService.getLoginUser(request);
        Boolean result = audioFileService.deleteFile(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }


    @PostMapping("/list/vo")
    public BaseResponse<Page<AudioFileVO>> listAudioFileVo(@RequestBody AudioFileQueryDto audioFileQueryDto,
                                                           HttpServletRequest request){
        ThrowUtils.throwIf(audioFileQueryDto==null, ErrorCode.PARAMS_ERROR,"参数为空");
        User loginUser = userService.getLoginUser(request);
        audioFileQueryDto.setUserId(loginUser.getId());
        int current = audioFileQueryDto.getCurrent();
        int pageSize = audioFileQueryDto.getPageSize();
        if(pageSize>50){
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR,"禁止爬虫");
        }
        Page<AudioFile> page = audioFileService.page(new Page<>(current, pageSize)
                , audioFileService.getQueryWrapper(audioFileQueryDto));
        Page<AudioFileVO> pageVO =audioFileService.listVO(page);

        return ResultUtils.success(pageVO);
    }
}
