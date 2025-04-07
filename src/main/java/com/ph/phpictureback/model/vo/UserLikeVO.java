package com.ph.phpictureback.model.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户点赞表
 * @TableName user_like
 */
@TableName(value ="user_like")
@Data
public class UserLikeVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 目标的类型 0-图片 1-帖子
     */
    private Integer targetType;

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
     * 是否点赞分享 0-点赞 1-分享
     */
    private Integer likeShare;

    /**
     * 我点赞的图片id的json数组
     */
    private List<PictureVO> likePic;

    /**
     * 我点赞的帖子id的json数组
     */
    private List<String> likePost;

    /**
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}