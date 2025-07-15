package com.ph.phpictureback.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.lmax.disruptor.WorkHandler;
import com.ph.phpictureback.manager.websocket.ChatHandler;
import com.ph.phpictureback.manager.websocket.PictureEditHandler;
import com.ph.phpictureback.manager.websocket.model.*;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 事件的消费者
 */

@Component
@Slf4j
public class ChatEventWorkHandler implements WorkHandler<ChatEvent> {

    @Resource
    private ChatHandler chatHandler;
    @Resource
    private UserService userService;


    @Override
    public void onEvent(ChatEvent chatEvent) throws Exception {
        //消息转化
        ChatRequestMessage chatRequestMessage = chatEvent.getChatRequestMessage();
        String type = chatRequestMessage.getType();
        ChatMessageTypeEnum enumValue = ChatMessageTypeEnum.valueOf(type);
        WebSocketSession session = chatEvent.getSession();
        User user = chatEvent.getUser();
        Long chatId = chatEvent.getChatId();
        switch (enumValue) {
            case SEND:
                chatHandler.handleChatMessage(chatRequestMessage, session, user, chatId);
                break;
            case HISTORY:
                chatHandler.sendHistoryMessages(session, chatId);
                break;
            case ONLINEUSER:
                chatHandler.broadcastOnlineUsers(chatId);
                break;
            default:
                log.error("未知消息类型: {}", type);
                break;
        }

    }
}
