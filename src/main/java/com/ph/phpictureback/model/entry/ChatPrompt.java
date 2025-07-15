package com.ph.phpictureback.model.entry;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 消息提示表
 * @TableName chat_prompt
 */
@TableName(value ="chat_prompt")
@Data
public class ChatPrompt implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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