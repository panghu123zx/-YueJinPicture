package com.ph.phpictureback.model.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户点赞表
 * @TableName user_like
 */
@TableName(value ="user_like")
@Data
public class UserLike implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 目标的类型 0-图片 1-帖子
     */
    private Integer targetType;

    /**
     * 是否点赞分享 0-点赞 1-分享
     */
    private Integer likeShare;

    /**
     * 用户 id
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
     * 我点赞的图片id的json数组
     */
    private String likePic;

    /**
     * 我点赞的帖子id的json数组
     */
    private String likePost;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}