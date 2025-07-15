package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptAddDto;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptQueryDto;
import com.ph.phpictureback.model.dto.chatPrompt.ChatPromptUpdateDto;
import com.ph.phpictureback.model.entry.ChatPrompt;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ChatPromptVO;

/**
* @author 杨志亮
* @description 针对表【chat_prompt(消息提示表)】的数据库操作Service
* @createDate 2025-07-13 17:12:19
*/
public interface ChatPromptService extends IService<ChatPrompt> {

    /**
     * 新增消息提示
     * @param chatPromptAddDto
     * @return
     */
    Long addChatPrompt(ChatPromptAddDto chatPromptAddDto);

    /**
     * 修改消息提示
     * @param chatUpdateAddDto
     * @return
     */
    Boolean updateChatPrompt(ChatPromptUpdateDto chatUpdateAddDto);

    QueryWrapper<ChatPrompt> getQueryWrapper(ChatPromptQueryDto chatPromptQueryDto);


    /**
     * 消息提示的VO类
     * @param page
     * @param loginUser
     * @return
     */
    Page<ChatPromptVO> listVO(Page<ChatPrompt> page, User loginUser);
}
