package com.ph.phpictureback.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.common.BaseResponse;
import com.ph.phpictureback.common.ResultUtils;
import com.ph.phpictureback.exception.BusinessException;
import com.ph.phpictureback.exception.ErrorCode;
import com.ph.phpictureback.exception.ThrowUtils;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptAddDto;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptQueryDto;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptUpdateDto;
import com.ph.phpictureback.model.entry.ChatPrompt;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ChatPromptVO;
import com.ph.phpictureback.service.ChatPromptService;
import com.ph.phpictureback.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 健康检查
 */
@RestController
@RequestMapping("/chatprompt")
public class ChatPromptController {

    @Resource
    private UserService userService;
    @Resource
    private ChatPromptService chatPromptService;
    /**
     * 获取消息提示列表VO
     * @param chatPromptQueryDto
     * @param request
     * @return
     */
    @PostMapping("/list/vo")
    public BaseResponse<Page<ChatPromptVO>> getListChatPromptVO(@RequestBody ChatPromptQueryDto chatPromptQueryDto
            , HttpServletRequest request){
        int current = chatPromptQueryDto.getCurrent();
        int pageSize = chatPromptQueryDto.getPageSize();
        User loginUser = userService.getLoginUser(request);
        if(pageSize>20){
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR,"禁止爬虫");
        }
        Page<ChatPrompt> page = chatPromptService.page(new Page<>(current, pageSize)
                , chatPromptService.getQueryWrapper(chatPromptQueryDto));

        Page<ChatPromptVO> pageVO=chatPromptService.listVO(page,loginUser);

        return ResultUtils.success(pageVO);
    }

    /**
     * 修改消息提示
     * @param chatPromptUpdateDto
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateChatPrompt(@RequestBody ChatPromptUpdateDto chatPromptUpdateDto
            , HttpServletRequest request){
        ThrowUtils.throwIf(chatPromptUpdateDto==null,ErrorCode.PARAMS_ERROR,"参数错误");
        userService.getLoginUser(request);
        return ResultUtils.success(chatPromptService.updateChatPrompt(chatPromptUpdateDto));
    }


    /**
     * 新增消息提示
     * @param chatPromptAddDto
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> updateChatPrompt(@RequestBody ChatPromptAddDto chatPromptAddDto
            , HttpServletRequest request){
        ThrowUtils.throwIf(chatPromptAddDto==null,ErrorCode.PARAMS_ERROR,"参数错误");
        userService.getLoginUser(request);
        return ResultUtils.success(chatPromptService.addChatPrompt(chatPromptAddDto));
    }

    /**
     * 清空未读消息数
     * @param chatPromptQueryDto
     * @param request
     * @return
     */
    @PostMapping("/clean")
    public BaseResponse<Boolean> cleanChatPrompt(@RequestBody ChatPromptQueryDto chatPromptQueryDto
            , HttpServletRequest request){
        ThrowUtils.throwIf(chatPromptQueryDto==null,ErrorCode.PARAMS_ERROR,"参数错误");
        userService.getLoginUser(request);
        Long id = chatPromptQueryDto.getId();
        //判断是否存在
        boolean exists = chatPromptService.lambdaQuery()
                .eq(ChatPrompt::getId, id)
                .exists();
        ThrowUtils.throwIf(!exists,ErrorCode.PARAMS_ERROR,"数据不存在");
        //更新未读消息数
        ChatPrompt chatPrompt = new ChatPrompt();
        chatPrompt.setId(id);
        chatPrompt.setUnreadCount(0);
        boolean update = chatPromptService.updateById(chatPrompt);
        ThrowUtils.throwIf(!update,ErrorCode.PARAMS_ERROR,"更新失败");
        return ResultUtils.success(true);
    }
}
