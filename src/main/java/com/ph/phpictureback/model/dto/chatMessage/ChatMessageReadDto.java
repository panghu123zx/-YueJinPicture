package com.ph.phpictureback.model.dto.chatMessage;

import lombok.Data;

import java.util.Date;

@Data
public class ChatMessageReadDto {

    private Long id;

    /**
     * 聊天发送者的id
     */
    private Long sendId;

    /**
     * 聊天接收者的id
     */
    private Long receiveId;

    /**
     * 读取消息的时间
     */
    private Date readTime;

}
