package com.ph.phpictureback.model.entry;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 评论
 * @TableName comment
 */
@TableName(value ="comment")
@Data
public class Comment implements Serializable {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目标id
     */
    private Long targetId;

    /**
     * 目标的类型 0-图片 1-帖子
     */
    private Integer targetType;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父级评论id
     */
    private Long parentId;

    /**
     * 点赞数量
     */
    private Integer likeCount;

    /**
     * 回复记录id
     */
    private Long fromId;

    /**
     * 回复人名称
     */
    private String fromName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除 1（true）已删除， 0（false）未删除
     */
    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}