package com.ph.phpictureback.model.dto.chatMessage;

import lombok.Data;

@Data
public class ChatMessageDeleteDto {
    /**
     * 聊天发送者的id
     */
    private Long sendId;

    /**
     * 聊天接收者的id
     */
    private Long receiveId;
}
