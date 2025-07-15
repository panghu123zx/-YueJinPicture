package com.ph.phpictureback.model.entry;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户聊天表
 * @TableName chat_message
 */
@TableName(value ="chat_message")
@Data
public class ChatMessage implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话id，链接为user1_user2用于区别会话的,id按大小排列
     */
    private String sessionId;

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
     * 是否已读， 0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 消息提示的id
     */
    private Long chatPromptId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}