package com.ph.phpictureback.model.entry;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 点赞/分享消息
 * @TableName like_message
 */
@TableName(value ="like_message")
@Data
public class LikeMessage implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息接收者id
     */
    private Long receiverId;

    /**
     * 消息发送者id
     */
    private Long sendId;

    /**
     * 目标的类型 0-图片 1-帖子
     */
    private Integer targetType;

    /**
     * 0-点赞，1-分享
     */
    private Integer actionType;

    /**
     * 目标的id
     */
    private Long targetId;

    /**
     * 是否已读， 0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 逻辑删除 1（true）已删除， 0（false）未删除
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}