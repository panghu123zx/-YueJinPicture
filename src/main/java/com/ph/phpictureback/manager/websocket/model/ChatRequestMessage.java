package com.ph.phpictureback.manager.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequestMessage {
    /**
     * 获取的类型：loadMore：加载更多，onlineUser：在线用户
     */
    private String type;
    /**
     * 发送的消息
     */
    private String content;

    /**
     * 消息的类型0-文本，1-图片
     */
    private Integer messageType;

    private Long targetId;

    /**
     * 回复消息
     */
    private Long replayId;

    /**
     * 消息的接收者
     */
    private Long receiverId;
    //todo
    private Long chatId;
}
