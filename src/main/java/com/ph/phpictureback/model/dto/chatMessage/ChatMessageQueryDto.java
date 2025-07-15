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

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 目标id
     */
    private Long targetUserId;

    /**
     * 聊天记录的名称
     */
    private String title;

    /**
     * 对方定义的聊天名称
     */
    private String receiveTitle;

    /**
     * 聊天类型 0-私信 ,1-好友，2-群聊
     */
    private Integer chatType;

    /**
     * 未读消息数量
     */
    private Integer unreadCount;

    /**
     * 最后一条消息内容
     */
    private String lastMessage;

    /**
     * 最后交流的时间
     */
    private Date lastMessageTime;

}
