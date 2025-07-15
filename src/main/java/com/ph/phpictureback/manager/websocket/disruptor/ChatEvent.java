package com.ph.phpictureback.manager.websocket.disruptor;

import com.ph.phpictureback.manager.websocket.model.ChatRequestMessage;
import com.ph.phpictureback.manager.websocket.model.PictureEditRequestMessage;
import com.ph.phpictureback.model.entry.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class ChatEvent {

    /**
     * 消息
     */
    private ChatRequestMessage chatRequestMessage;

    private WebSocketSession session;
    
    /**
     * 当前用户
     */
    private User user;

    /**
     * 聊天室 id
     */
    private Long chatId;

}
