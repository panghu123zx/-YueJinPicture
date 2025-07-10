package com.ph.phpictureback.model.dto.follow;

import com.ph.phpictureback.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 创建请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FollowQueryDto extends PageRequest implements Serializable {
    private Long id;

    /**
     * 被关注的人的id
     */
    private Long userId;

    /**
     * 粉丝id
     */
    private Long followerId;

    /**
     * 是否双向关注， 0-否，1-是
     */
    private Integer isMutual;

    /**
     * 关注状态， 0-关注，1-取消关注
     */
    private  Integer followState=0;

    /**
     * 查询状态，0-查询我关注的，1-查询关注我的
     */
    private Integer queryStatus;


    private static final long serialVersionUID = 1L;
}