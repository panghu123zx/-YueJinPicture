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
     * 获取的类型
     */
    private String type;
    /**
     * 发送的消息
     */
    private String content;

    /**
     * 消息的类型0-图片，1-文件 ,null为文本
     */
    private Integer messageType;

    /**
     * 消息类型的目标ID
     */
    private Long targetId;

    /**
     * 回复消息
     */
    private Long replayId;

    /**
     * 消息的接收者
     */
    private Long receiverId;
}
