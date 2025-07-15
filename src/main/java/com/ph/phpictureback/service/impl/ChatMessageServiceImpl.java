package com.ph.phpictureback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ph.phpictureback.manager.websocket.model.ChatResponseMessage;
import com.ph.phpictureback.model.entry.ChatMessage;
import com.ph.phpictureback.mapper.ChatMessageMapper;
import com.ph.phpictureback.service.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 杨志亮
* @description 针对表【chat_message(用户聊天表)】的数据库操作Service实现
* @createDate 2025-07-13 17:12:12
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService {


    /**
     * 获取历史消息
     * @param id
     * @param current
     * @param pageSize
     * @return
     */
    @Override
    public Page<ChatMessage> getHistoryMessages(Long id, Long current, Long pageSize) {
        //todo 使用缓存
        QueryWrapper<ChatMessage> qw = new QueryWrapper<>();
        qw.eq("chatPromptId",id).orderByAsc("createTime");
        Page<ChatMessage> page = this.page(new Page<>(current, pageSize), qw);
        return page;
    }
}




