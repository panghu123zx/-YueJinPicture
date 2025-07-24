package com.ph.phpictureback.model.dto.chatMessage;


import lombok.Data;

import java.util.Date;

@Data
public class ChatMessageQueryDto {

    /**
     * id
     */
    private Long id;

    /**
     * 回复消息的id
     */
    private Long replayId;

    /**
     * 聊天发送者的id
     */
    private Long sendId;

    /**
     * 聊天接收者的id
     */
    private Long receiveId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型 0-图片，1-文件
     */
    private Integer messageType;

    /**
     * 目标的id
     */
    private Long targetId;


}
