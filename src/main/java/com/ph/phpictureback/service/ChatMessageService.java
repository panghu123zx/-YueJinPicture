package com.ph.phpictureback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ph.phpictureback.manager.websocket.model.ChatResponseMessage;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.vo.ChatMessageVO;

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
    Page<ChatMessageVO> getHistoryMessages(Long id, Long current, Long pageSize);

    /**
     * 撤回消息
     * @param id
     * @param loginUser
     * @return
     */
    boolean backMessage(Long id, User loginUser);

    /**
     * 更新缓存
     * @param id
     * @param chatMessage
     */
    void updateChatCache(Long id,ChatMessage chatMessage);
}
