package com.ph.phpictureback.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageAddDto;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageQueryDto;
import com.ph.phpictureback.model.dto.likeMessage.LikeMessageReadDto;
import com.ph.phpictureback.model.entry.LikeMessage;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.LikeMessageVO;
import com.ph.phpictureback.service.LikeMessageService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 消息
 * */
@RestController
@RequestMapping("/message")
public class LikeMessageController {
    @Resource
    private UserService userService;
    @Resource
    private LikeMessageService likeMessageService;

    /**
     * 发送消息
     * @param likeMessageAddDto
     * @param request
     * @return
     */
    @PostMapping("/send")
    public BaseResponse<Long> addLikeMessage(@RequestBody LikeMessageAddDto likeMessageAddDto, HttpServletRequest request) {
        ThrowUtils.throwIf(likeMessageAddDto ==null, ErrorCode.PARAMS_ERROR,"参数错误");
        User loginUser = userService.getLoginUser(request);
        Long id=likeMessageService.addLikeMessage(likeMessageAddDto,loginUser);
        return ResultUtils.success(id);
    }

    /**
     * 读取消息
     * @param likeMessageReadDto
     * @param request
     * @return
     */
    @PostMapping("/read")
    public BaseResponse<Boolean> readMessage(@RequestBody LikeMessageReadDto likeMessageReadDto, HttpServletRequest request) {
        ThrowUtils.throwIf(likeMessageReadDto ==null, ErrorCode.PARAMS_ERROR,"参数错误");
        userService.getLoginUser(request);
        boolean read = likeMessageService.readMessage(likeMessageReadDto);
        return ResultUtils.success(read);
    }

    /**
     * 获取消息列表
     * @param likeMessageQueryDto
     * @param request
     * @return
     */
    @PostMapping("/list/vo")
    public BaseResponse<Page<LikeMessageVO>> listLikeMessageVO(@RequestBody LikeMessageQueryDto likeMessageQueryDto, HttpServletRequest request) {
        ThrowUtils.throwIf(likeMessageQueryDto ==null, ErrorCode.PARAMS_ERROR,"参数错误");
        User loginUser = userService.getLoginUser(request);
        int current = likeMessageQueryDto.getCurrent();
        int pageSize = likeMessageQueryDto.getPageSize();

        Page<LikeMessage> page = likeMessageService.page(new Page<>(current, pageSize)
                , likeMessageService.getQueryWrapper(likeMessageQueryDto));
        Page<LikeMessageVO> pageVoList = likeMessageService.listLikeMessageVO(page, loginUser);
        return ResultUtils.success(pageVoList);
    }


}
