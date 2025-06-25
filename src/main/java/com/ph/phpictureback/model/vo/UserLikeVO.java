package com.ph.phpictureback.model.vo;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ph.phpictureback.model.entry.Forum;
import com.ph.phpictureback.model.entry.User;
import com.ph.phpictureback.model.entry.UserLike;
import lombok.Data;
import org.springframework.beans.BeanUtils;

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
    private List<String> likePic;

    /**
     * 我点赞的图片id的json数组
     */
    private List<PictureVO> likePicVO;

    /**
     * 我点赞的帖子id的json数组
     */
    private List<String> likePost;

    /**
     * 我分享/点赞的帖子 的数组
     */
    private List<ForumVO> likePostVO;

    /**
     * 创建时间
     */
    private Date createTime;


    /**
     * 包装类转对象
     *
     * @param userLikeVO
     * @return
     */
    public static UserLike voToObj(UserLikeVO userLikeVO) {
        if (userLikeVO == null) {
            return null;
        }
        UserLike userLike = new UserLike();
        BeanUtils.copyProperties(userLikeVO, userLike);
        //TODO
        userLike.setLikePic(JSONUtil.toJsonStr(userLikeVO.getLikePic()));
        userLike.setLikePost(JSONUtil.toJsonStr(userLikeVO.getLikePost()));
        return userLike;
    }

    /**
     * 对象转包装类
     *
     * @param userLike
     * @return
     */
    public static UserLikeVO objToVo(UserLike userLike) {
        if (userLike == null) {
            return null;
        }
        UserLikeVO userLikeVO = new UserLikeVO();
        BeanUtils.copyProperties(userLike, userLikeVO);
        userLikeVO.setLikePost(JSONUtil.toList(userLike.getLikePost(), String.class));
        userLikeVO.setLikePic(JSONUtil.toList(userLike.getLikePic(), String.class));
        return userLikeVO;
    }

    private static final long serialVersionUID = 1L;
}