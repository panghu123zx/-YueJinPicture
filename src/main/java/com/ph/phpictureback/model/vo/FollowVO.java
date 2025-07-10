package com.ph.phpictureback.model.vo;

import com.ph.phpictureback.model.entry.Follow;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 脱敏后关注表
 */
@Data
public class FollowVO implements Serializable {

    /**
     * 主键id
     */
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
     * 关注状态， 0-已关注，1-已取消关注
     */
    private  Integer followState;

    /**
     * 创建时间
     */
    private Date createTime;


    /**
     * 被关注的用户
     */
    private UserVO userVO;

    /**
     * 粉丝
     */
    private UserVO followerVO;

    /**
     * 包装类转对象
     *
     * @param followVO
     * @return
     */
    public static Follow voToObj(FollowVO followVO) {
        if (followVO == null) {
            return null;
        }
        Follow follow = new Follow();
        BeanUtils.copyProperties(followVO, follow);

        return follow;
    }

    /**
     * 对象转包装类
     *
     * @param follow
     * @return
     */
    public static FollowVO objToVo(Follow follow) {
        if (follow == null) {
            return null;
        }
        FollowVO followVO = new FollowVO();
        BeanUtils.copyProperties(follow, followVO);
        return followVO;
    }

    private static final long serialVersionUID = 1L;
}
