package com.ph.phpictureback.model.dto.userlike;

import lombok.Data;

import java.io.Serializable;

/**
 * 点赞
 */
@Data
public class UserLikeAddDto implements Serializable {

    /**
     * 点赞目标id
     */
    private Long targetId;

    /**
     * 点赞类型 0:图片 1:帖子
     */
    private Integer targetType;

    /**
     * 是否点赞分享 0-点赞 1-分享
     */
    private Integer likeShare = 0;

    private static final long serialVersionUID = 1L;
}
