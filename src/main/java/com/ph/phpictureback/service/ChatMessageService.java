package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.manager.websocket.model.ChatResponseMessage;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 杨志亮
* @description 针对表【chat_message(用户聊天表)】的数据库操作Service
* @createDate 2025-07-13 17:12:12
*/
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     *
     * @param id
     * @param current
     * @param pageSize
     * @return
     */
    Page<ChatMessage> getHistoryMessages(Long id, Long current, Long pageSize);
}
