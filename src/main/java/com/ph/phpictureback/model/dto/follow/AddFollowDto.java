package com.ph.phpictureback.model.dto.follow;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 *
 */
@Data
public class AddFollowDto implements Serializable {

    /**
     * 被关注的人的id
     */
    private Long userId;

    /**
     * 粉丝id
     */
    private Long followerId;

    /**
     * 0-添加关注， 1-取消关注
     */
    private Integer status=0;



    private static final long serialVersionUID = 1L;
}