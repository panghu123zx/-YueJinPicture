package com.ph.phpictureback.model.dto.chatPrompt;

import lombok.Data;

import java.util.Date;

@Data
public class ChatPromptAddDto {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 目标id
     */
    private Long targetId;

    /**
     * 聊天记录的名称
     */
    private String title;

    /**
     * 对方定义的聊天名称
     */
    private String receiveTitle;

    /**
     * 聊天类型 0-私信 ,1-好友，2-群聊，3-普通
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
