package com.ph.phpictureback.model.vo;

import com.ph.phpictureback.model.entry.LikeMessage;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;

@Data
public class LikeMessageVO {
    /**
     * 主键id
     */
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
     * 消息发送者
     */
    private UserVO sendUser;

    /**
     * 图片信息
     */
    private PictureVO picture;

    /**
     * 帖子信息
     */
    private ForumVO forumVO;

    /**
     * 包装类转对象
     *
     * @param likeMessageVO
     * @return
     */
    public static LikeMessage voToObj(LikeMessageVO likeMessageVO) {
        if (likeMessageVO == null) {
            return null;
        }
        LikeMessage likeMessage = new LikeMessage();
        BeanUtils.copyProperties(likeMessageVO, likeMessage);

        return likeMessage;
    }

    /**
     * 对象转包装类
     *
     * @param likeMessage
     * @return
     */
    public static LikeMessageVO objToVo(LikeMessage likeMessage) {
        if (likeMessage == null) {
            return null;
        }
        LikeMessageVO likeMessageVO = new LikeMessageVO();
        BeanUtils.copyProperties(likeMessage, likeMessageVO);
        return likeMessageVO;
    }
}
