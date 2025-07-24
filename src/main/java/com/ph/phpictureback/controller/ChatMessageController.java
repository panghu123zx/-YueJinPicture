package com.ph.phpictureback.controller;

import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.DeleteRequest;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.service.ChatMessageService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/chatmessage")
public class ChatMessageController {

    @Resource
    private UserService userService;
    @Resource
    private ChatMessageService chatMessageService;

    /**
     *数据库新增测回字段，方便与前端展示测回信息
     * 撤回消息
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> backMessage(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request)  {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId()<=0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean update = chatMessageService.backMessage(deleteRequest.getId(), loginUser);
        return ResultUtils.success(update);
    }

}
